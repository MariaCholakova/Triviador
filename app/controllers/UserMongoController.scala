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
    def getUser(username: String): Future[User] = {
        lookupUser(username) map {
            option => option.get
        }
    }

    def lookupUser(username: String, password: String = ""): Future[Option[User]] = {
        val query = if (password != "") Json.obj("username" -> username,"password" -> password)
            else Json.obj("username" -> username)
        val userOption : Future[Option[User]] = for {
            col <- userCollection
            option <- col.find(query).one[User]
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
        for {
            user <- getUser(username)
            val newPoints = user.points + pointsToAdd
            col <- userCollection
            updateRes = col.update.one(selector,modifier(newPoints), upsert = false, multi = false)
        } yield newPoints
    }

    def getUserList(query: JsObject) : Future[List[User]] = for {
        col <- userCollection
        users <- col.find(query)
            .sort(Json.obj("points" -> -1))
            .cursor[User]()
            .collect[List](-1, Cursor.FailOnError[List[User]]())
    } yield users

    def getRankList = Action.async {
        getUserList(Json.obj()) map {
            ul => Ok(views.html.ranking(ul))
        } recover {
            case _ => Ok(views.html.error())
        }    
    }

    implicit val newsFormat = Json.format[News]
    def getNewsForUser(user: String) : Future[List[News]] = {
        val newsList = for {
            col <- newsCollection
            news <- col.find(Json.obj("user" -> user))
                .sort(Json.obj("date" -> -1))
                .cursor[News]()
                .collect[List](-1, Cursor.FailOnError[List[News]]())
        } yield news
        newsList    
    }

    def insertNewsForUser(user: String, text: String) = {
        val newsToInsert = Json.obj(
            "user" -> user,
            "text" -> text,
            "date" -> System.currentTimeMillis(),
        )
        for {
            col <- newsCollection
            insertRes <- col.insert(newsToInsert)
        } yield insertRes
    }

    // def bulkInsertNews(usernames: List[String], text: String) = {
    //     val newsToInsert = usernames.map(user => Json.obj(
    //         "user" -> user,
    //         "text" -> text,
    //         "date" -> Instant.now.getEpochSecond,
    //     ))
    //     for {
    //         col <- newsCollection
    //         insertRes <- col.bulkInsert(newsToInsert)
    //     } yield insertRes
    // }

}