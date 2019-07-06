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
import scala.concurrent.{ Future, ExecutionContext }
import scala.util.Random

import models.{Global, User, News}

case class Question(category: String, `type`: String, difficulty: String, question: String,
  correct_answer: String, incorrect_answers: List[String] )
case class QuestionParam(difficulty: String, choice: String)
case class Answer(text: String, isCorrect: Int)
case class AnswerParam(difficulty: String, isCorrect: Int)


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
    def showLandingPage() = authenticatedUserAction { implicit request: Request[AnyContent] =>
      // fetch new for the user
      Ok(views.html.loginLandingPage(List[News](), logoutUrl))
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
                val answers = qs(0).incorrect_answers.map(str => Answer(str, 0)) ++ 
                  List(Answer(qs(0).correct_answer, 1))
                Ok(views.html.question(qs(0), Random.shuffle(answers)))
              }
              case e: JsError => Ok(views.html.error())
            }
            case _ => Ok(views.html.error())
          }
          
        }
      )

  }

  val answerForm: Form[AnswerParam] = Form {
        mapping (
        "difficulty" -> text,
        "isCorrect" -> number
        ) (AnswerParam.apply _) (AnswerParam.unapply _)
    }
  def processAnswer = Action.async { implicit request: Request[AnyContent] =>
    val answer =  answerForm.bindFromRequest.get
    val pointsWon = answer.difficulty match {
      case "easy" => if (answer.isCorrect == 1) 5 else 0
      case "medium" => if (answer.isCorrect == 1) 10 else 0
      case "hard" => if (answer.isCorrect == 1) 15 else 0
      case _ => 0
    }
    val pointsOption = for {
      username <- request.session.get(models.Global.SESSION_USERNAME_KEY)
    } yield for {
      points <- userMongoController.increasePoints(username, pointsWon)
    } yield points
  
    pointsOption match {
      case Some(pf) => pf map {
        p => Ok(views.html.answer(if(answer.isCorrect == 1) "correct" else "wrong", p))
      }
      case None => Future.successful(Ok(views.html.error()))
    } 
    
  }


}

