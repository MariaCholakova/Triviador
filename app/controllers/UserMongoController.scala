package controllers
import javax.inject.Inject
import models.{ Global, User, LoginUser }

import scala.concurrent.{ ExecutionContext, Future }

import play.api.Logger
import play.api.mvc.{ AbstractController, ControllerComponents }
import play.api.libs.functional.syntax._
import play.api.libs.json._

// Reactive Mongo imports
import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}

import reactivemongo.play.json._, collection._

@javax.inject.Singleton
class UserMongoController @Inject()(
    cc: ControllerComponents,
    implicit val ec : ExecutionContext,
    val reactiveMongoApi: ReactiveMongoApi,
) extends AbstractController(cc) with MongoController with ReactiveMongoComponents {
    
    def collection: Future[JSONCollection] =
        database.map(_.collection[JSONCollection]("users"))

    implicit val userFormat = Json.format[User]
    // implicit val userReads: Reads[User] = (
    //     (JsPath \ "username").read[String] and
    //     (JsPath \ "password").read[String] and
    //     (JsPath \ "points").read[Int]
    // )(User.apply _)

    def lookupUser(user: LoginUser): Future[Boolean] = {
        val userOption : Future[Option[User]] = for {
            col <- collection
            option <- col.find(Json.obj(
                "username" -> user.username,
                "password" -> user.password)).one[User]
        } yield option
        userOption map {
            case Some(User(_,_,_)) => {
                println("yes")
                true
            }
            case None => false
        }
    }

    def increasePoints(username: String, pointsToAdd: Int) : Future[Int] = {

        def modifier(points: Int) = Json.obj (
            "$set" -> Json.obj (
                "points" -> points,
            )
        )
        val selector = Json.obj("username" -> username)

        val userOption = for {
            col <- collection
            findOption <- col.find(selector).one[User]
        } yield findOption

        userOption map {
            case Some(User(a, b, points)) => {
                val newPoints = points + pointsToAdd
                println(newPoints)
                collection.map(_.update.one(selector, modifier(newPoints),
                    upsert = false, multi = false
                ))
                newPoints
            }
            case None => 0
        }
    
    }
  

}