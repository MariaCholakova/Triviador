package controllers
import javax.inject.Inject
import models.{Global, User}

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
    def lookupUser(user: User): Future[Boolean] = {
        val userOption = for {
            col <- collection
            option <- col.find(Json.obj(
                "username" -> user.username,
                "password" -> user.password)).one[User]
        } yield option
        userOption map {
            case Some(User(a, b)) => true
            case None => false
        }
    }
  

}