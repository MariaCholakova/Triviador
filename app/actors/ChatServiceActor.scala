package actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import play.api.libs.json._

object ChatServiceActor {
  def props(out: ActorRef) = Props(new ChatServiceActor(out))
}

class ChatServiceActor(out: ActorRef) extends Actor {
  def receive: Receive = {
    case msg: String => {
      val jsonMsg = Json.parse(msg)
      val receiver = (jsonMsg \ "receiver").as[String]
      println(receiver)
      val text = (jsonMsg \ "text").as[String]
      out ! text
    }
  }

  override def postStop() {
    println("Closing the websocket connection.")
  }
}