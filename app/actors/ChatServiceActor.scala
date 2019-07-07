package actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

object ChatServiceActor {
  def props(out: ActorRef) = Props(new ChatServiceActor(out))
}

class ChatServiceActor(out: ActorRef) extends Actor {
  def receive: Receive = {
    case msg: String => {
      println (msg)
      out ! msg
    }
  }

  override def postStop() {
    println("Closing the websocket connection.")
  }
}