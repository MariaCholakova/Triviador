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
import scala.concurrent.{ Future, ExecutionContext, Await }
import scala.util.Random

import models.{Global, User, News}

case class Question(category: String, `type`: String, difficulty: String, question: String,
  correct_answer: String, incorrect_answers: List[String] )
case class QuestionParam(difficulty: String, choice: String)
case class Answer(text: String, isCorrect: Int)
case class AnswerParam(difficulty: String, isCorrect: Int)
case class NoUserFoundException(id: Long) extends Exception


@Singleton
class LandingPageController @Inject()(
    ws: WSClient,
    implicit val ec : ExecutionContext,
    cc: ControllerComponents,
    authenticatedUserAction: AuthenticatedUserAction,
    userMongoController: UserMongoController
) extends AbstractController(cc) {

  private val logoutUrl = routes.AuthenticatedUserController.logout

  // this is where the user comes immediately after logging in.
  // notice that this uses `authenticatedUserAction`.
  def showLandingPage() = authenticatedUserAction.async { implicit request: Request[AnyContent] =>
    // fetch new for the user
    val userNews = for {
      username <- Future.successful(request.session.get(models.Global.SESSION_USERNAME_KEY))
      news <- userMongoController.getNewsForUser(username.get)
    } yield news
    userNews.map {
      news => Ok(views.html.loginLandingPage(news, logoutUrl))
    } recover {
      case _ => Ok(views.html.error())
    }
  }

  private def getRandomElement(list: Seq[Int], random: Random): Int = 
    list(random.nextInt(list.length))

  implicit val questionReads = Json.reads[Question]
  val questionForm: Form[QuestionParam] = Form {
    mapping (
    "difficulty" -> text,
    "choice" -> text
    ) (QuestionParam.apply _) (QuestionParam.unapply _)
  }

  def question = authenticatedUserAction.async { implicit request: Request[AnyContent] =>
    val questionParam =  questionForm.bindFromRequest.get
    val url = "https://opentdb.com/api.php"
    for {
      resp <- ws.url(url).addQueryStringParameters(
        "amount" -> "1",
        "category" ->  getRandomElement(List.range(17,29,1), new Random).toString,
        "difficulty" -> questionParam.difficulty,
        "type" -> questionParam.choice
      ).get() 
      val responseCode = (resp.json \ "response_code").as[Int]
      val page = responseCode match { 
        case 0 => {
          val questionsList = (resp.json \ "results").as[List[Question]]
          val question = questionsList(0)
          val answers = question.incorrect_answers
            .map(text => Answer(text, 0)) ++ List(Answer(question.correct_answer, 1))
          Ok(views.html.question(question, Random.shuffle(answers)))
        }
        case _ => Ok(views.html.error())
      }
    } yield page
  }

  val answerForm: Form[AnswerParam] = Form {
    mapping (
    "difficulty" -> text,
    "isCorrect" -> number
    ) (AnswerParam.apply _) (AnswerParam.unapply _)
  }
  def processAnswer = Action.async { implicit request: Request[AnyContent] =>
    val answer =  answerForm.bindFromRequest.get

    answer.isCorrect match {
      case 1 => {
        val pointsWon = answer.difficulty match {
          case "easy" => 5
          case "medium" => 10 
          case "hard" => 15
          case _ => 0
        }
        for {
          username <- Future.successful(request.session.get(models.Global.SESSION_USERNAME_KEY))
          val thisUser = username.get
          newPoints <- userMongoController.increasePoints(thisUser, pointsWon)
          allBehind <- userMongoController.getUserList(Json.obj("points" -> Json.obj("$lt" -> newPoints)))
          val outrunUsers = allBehind
            .filter(user => newPoints - user.points <= pointsWon && user.username != thisUser)
            .map(_.username)
          inserts <- Future.sequence(
            outrunUsers.map(user => 
              userMongoController.insertNewsForUser(user, s"User $thisUser moved ahead of you in the rank list :(")
            )
          ) 
        } yield Ok(views.html.answer("correct", newPoints))
      }     
      
      case 0 => for {
        username <- Future.successful(request.session.get(models.Global.SESSION_USERNAME_KEY))
        user <- userMongoController.getUser(username.get)
      } yield Ok(views.html.answer("wrong", user.points))
      
    }
  }

}

