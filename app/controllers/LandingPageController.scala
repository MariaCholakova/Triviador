package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.libs.ws._
import play.api.http.HttpEntity
import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.ExecutionContext
import scala.util.Random

case class Question(category: String, `type`: String, difficulty: String, question: String,
  correct_answer: String, incorrect_answers: List[String] )
case class QuestionParam(difficulty: String, choice: String)


@Singleton
class LandingPageController @Inject()(
    ws: WSClient,
    implicit val ec : ExecutionContext,
    cc: ControllerComponents,
    authenticatedUserAction: AuthenticatedUserAction
) extends AbstractController(cc) {

    private val logoutUrl = routes.AuthenticatedUserController.logout

    // this is where the user comes immediately after logging in.
    // notice that this uses `authenticatedUserAction`.
    def showLandingPage() = authenticatedUserAction { implicit request: Request[AnyContent] =>
        Ok(views.html.loginLandingPage(logoutUrl))
    }

    def getRandomElement(list: Seq[Int], random: Random): Int = 
    list(random.nextInt(list.length))

    val questionForm: Form[QuestionParam] = Form {
        mapping (
        "difficulty" -> text,
        "choice" -> text
        ) (QuestionParam.apply _) (QuestionParam.unapply _)
    }

  implicit val questionReads = Json.reads[Question]

  def question = Action.async { implicit request: Request[AnyContent] =>
    val questionParam =  questionForm.bindFromRequest.get
    println(questionParam)
    val url = "https://opentdb.com/api.php"
    ws.url(url).addQueryStringParameters(
        "amount" -> "1",
        "category" ->  getRandomElement(List.range(17,29,1), new Random).toString,
        "difficulty" -> questionParam.difficulty,
        "type" -> questionParam.choice
      ).get().map(
        resp => {
          (resp.json \ "response_code").validate[Int].get match {
            case 0 => (resp.json \ "results").validate[List[Question]] match {
              case s: JsSuccess[List[Question]] => {
                val qs: List[Question] = s.get
                println(qs)
                Ok(views.html.question(qs(0)))
              }
              case e: JsError => Ok(views.html.error())
            }
            case _ => Ok(views.html.error())
          }
          
        }
      )

  }

}

