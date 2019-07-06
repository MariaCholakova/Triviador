package controllers

import javax.inject.Inject
import models.{Global, LoginUser, User}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scala.concurrent.{ ExecutionContext, Future , Await}
import scala.concurrent.duration._

class LoginController @Inject()(
    cc: MessagesControllerComponents,
    implicit val ec : ExecutionContext,
    userMongoController: UserMongoController
) extends MessagesAbstractController(cc) {

    private val logger = play.api.Logger(this.getClass)

    val form: Form[LoginUser] = Form (
        mapping(
            "username" -> nonEmptyText
                .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
                .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 20)),
            "password" -> nonEmptyText
                .verifying("too few chars",  s => lengthIsGreaterThanNCharacters(s, 2))
                .verifying("too many chars", s => lengthIsLessThanNCharacters(s, 30)),
        )(LoginUser.apply)(LoginUser.unapply)
    )

    private val formSubmitUrl = routes.LoginController.processLoginAttempt

    def showLoginForm = Action { implicit request: MessagesRequest[AnyContent] =>
        Ok(views.html.userLogin(form, formSubmitUrl))
    }

    def processLoginAttempt = Action { implicit request: MessagesRequest[AnyContent] =>
        val errorFunction = { formWithErrors: Form[LoginUser] =>
            // form validation/binding failed...
            BadRequest(views.html.userLogin(formWithErrors, formSubmitUrl))
        }
        val successFunction = { user: LoginUser =>
            // form validation/binding succeeded ...
            val foundUser : Option[User] = 
                Await.result(userMongoController.lookupUser(user.username, user.password), Duration.Inf)
            foundUser match {
                case Some(User(name, _, points)) => Redirect(routes.LandingPageController.showLandingPage)
                    .flashing("info" -> s"Dear $name, welcome to the game!\nYou have $points points")
                    .withSession(Global.SESSION_USERNAME_KEY -> user.username)
                case None => Redirect(routes.LoginController.showLoginForm)
                    .flashing("error" -> "Invalid username/password.")
            }
        }
        val formValidationResult: Form[LoginUser] = form.bindFromRequest
        formValidationResult.fold(
            errorFunction,
            successFunction
        )
    }

    private def lengthIsGreaterThanNCharacters(s: String, n: Int): Boolean = {
        if (s.length > n) true else false
    }

    private def lengthIsLessThanNCharacters(s: String, n: Int): Boolean = {
        if (s.length < n) true else false
    }

}
