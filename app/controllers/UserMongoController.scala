package controllers
import javax.inject.Inject
import models.{ Global, User, LoginUser, News }

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
    
    def userCollection: Future[JSONCollection] =
        database.map(_.collection[JSONCollection]("users"))
    def newsCollection: Future[JSONCollection] =
        database.map(_.collection[JSONCollection]("news"))

    implicit val userFormat = Json.format[User]
    def lookupUser(user: LoginUser): Future[Option[User]] = {
        val userOption : Future[Option[User]] = for {
            col <- userCollection
            option <- col.find(Json.obj(
                "username" -> user.username,
                "password" -> user.password)).one[User]
        } yield option
        userOption
    }

    def increasePoints(username: String, pointsToAdd: Int) : Future[Int] = {

        def modifier(points: Int) = Json.obj (
            "$set" -> Json.obj (
                "points" -> points,
            )
        )
        val selector = Json.obj("username" -> username)

        val userOption = for {
            col <- userCollection
            findOption <- col.find(selector).one[User]
        } yield findOption

        userOption map {
            case Some(User(a, b, points)) => {
                val newPoints = points + pointsToAdd
                println(newPoints)
                userCollection.map(_.update.one(selector, modifier(newPoints),
                    upsert = false, multi = false
                ))
                newPoints
            }
            case None => 0
        }
    }

    def getRankList = Action.async {
        val userList = for {
            col <- userCollection
            users <- col.find(Json.obj())
                .sort(Json.obj("points" -> -1))
                .cursor[User]()
                .collect[List](-1, Cursor.FailOnError[List[User]]())
        } yield users
        userList map {
            ul => Ok(views.html.ranking(ul))
        } recover {
            case _ => Ok(views.html.error())
        }
            
    }  

    implicit val newsFormat = Json.format[News]
    def getNewsForUser(userId: JsObject) : Future[List[News]] = {
        val newsList = for {
            col <- newsCollection
            news <- col.find(Json.obj("user_id" -> userId))
                .sort(Json.obj("date" -> -1))
                .cursor[News]()
                .collect[List](-1, Cursor.FailOnError[List[News]]())
        } yield news
        newsList    
    }  
  

}