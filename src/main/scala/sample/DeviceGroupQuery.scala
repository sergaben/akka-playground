package sample

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

/**
  *
  * @project akka-sample-main-scala
  * @author sergaben on 27/09/2018.
  *
  */
object DeviceGroupQuery{

  case object CollectionTimeout

  def props(
             actorToDeviceId: Map[ActorRef, String],
             requestId: Long,
             requester: ActorRef,
             timeout: FiniteDuration ) : Props = {
      Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))
  }
}

class DeviceGroupQuery(
       actorToDeviceId: Map[ActorRef, String],
       requestId: Long,
       requester: ActorRef,
       timeout: FiniteDuration
                      ) extends Actor with ActorLogging{

  import DeviceGroupQuery._
  import context.dispatcher

  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach{ deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(0)
    }
  }

 override def postStop(): Unit = {
   queryTimeoutTimer.cancel()
 }

  // this receive function will receive the messages and act on them using another custom function.
  override def receive : Receive = {
    waitingForReplies(
      Map.empty,
      actorToDeviceId.keySet
    )
  }
  // this function will take care of the messages sent to this actor but not directly as it returns a function Receive which will take care of it
  def waitingForReplies(repliesSoFar: Map[String, DeviceGroup.TemperatureReading], stillWaiting: Set[ActorRef]): Receive ={
    case Device.RespondTemperature(0, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => DeviceGroup.Temperature(value)
        case None => DeviceGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor,DeviceGroup.DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies: Set[(String, DeviceGroup.DeviceTimeOut.type)] =
        stillWaiting.map { deviceActor =>
          val deviceId  = actorToDeviceId(deviceActor)
          deviceId -> DeviceGroup.DeviceTimeOut
        }
      requester ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }
}
