package controllers

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.google.inject.Singleton
import javax.inject.Inject
import play.api.libs.streams.ActorFlow
import play.api.mvc.{AbstractController, ControllerComponents, WebSocket}
import actors.ChatServiceActor

/**
  * Chat Controller instance that handles the incoming chat requests with the server and provides a Echo chat service.
  *
  */
@Singleton
class ChatController @Inject()(cc: ControllerComponents)
    (implicit system: ActorSystem, mat: Materializer)
  extends AbstractController(cc) {

  /**
   * Creates a websocket.  `acceptOrResult` is preferable here because it returns a
   * Future[Flow], which is required internally.
   *
   * @return a fully realized websocket.
   */
  def socket : WebSocket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      ChatServiceActor.props(out)
    }
  }
}