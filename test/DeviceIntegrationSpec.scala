import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sample.{Device, DeviceGroup, DeviceManager}

/**
  *
  * @project akka-sample-main-scala
  * @author sergaben on 29/09/2018.
  *
  */
class DeviceIntegrationSpec extends TestKit(ActorSystem("deviceGroupQuerySpec")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "The integration between all classes" must {
    "be able to collect temperatures from all actives devices" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"),probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val deviceActor1 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val deviceActor2 = probe.lastSender

      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device3"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val deviceActor3 = probe.lastSender // not expecting any temperature from this device

      // check the device actor are working
      deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
      deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

      groupActor.tell(DeviceGroup.RequestAllTemperatures(requestId = 0), probe.ref)
      probe.expectMsg(
        DeviceGroup.RespondAllTemperatures(
          requestId = 0,
          temperatures = Map(
            "device1" -> DeviceGroup.Temperature(1.0),
            "device2" -> DeviceGroup.Temperature(2.0),
            "device3" -> DeviceGroup.TemperatureNotAvailable
          )
        )
      )

    }
  }
}
