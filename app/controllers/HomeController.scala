package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._

import play.api.libs.ws._
import play.api.http.HttpEntity
import play.api.data.Form
import play.api.data.Forms._
import scala.concurrent.ExecutionContext

case class Question(category: String, `type`: String, difficulty: String, question: String,
  correct_answer: String, incorrect_answers: List[String] )
case class QuestionParam(difficulty: String, choice: String)
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(ws: WSClient, implicit val ec:ExecutionContext,  cc: ControllerComponents) extends AbstractController(cc) {
  //implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }


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
