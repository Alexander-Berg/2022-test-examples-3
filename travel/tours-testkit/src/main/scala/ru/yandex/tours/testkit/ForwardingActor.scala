package ru.yandex.tours.testkit

import akka.actor.{ActorRef, Props, Actor}
import akka.testkit.TestProbe

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 30.12.14
 */
class ForwardingActor(recipient: ActorRef) extends Actor {
  override def receive: Receive = {
    case msg => recipient forward msg
  }
}

object ForwardingActor {
  def props(recipient: ActorRef): Props = Props(new ForwardingActor(recipient))
  def props(probe: TestProbe): Props = props(probe.ref)
}