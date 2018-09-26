package sample

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.io.StdIn

object IotSupervisor {
  def props(): Props = Props(new IotSupervisor)
}

class IotSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("IoT Application started")
  override def postStop(): Unit = log.info("IoT Application stopped")

  // No need to handle any messages
  override def receive = Actor.emptyBehavior

}

object IotSupervisorLogging extends App {
  val system = ActorSystem("iot-system")

  try{
    val supervisor = system.actorOf(Props[IotSupervisor],"iot-supervisor")

    StdIn.readLine()
  }finally{
    system.terminate()
  }
}
