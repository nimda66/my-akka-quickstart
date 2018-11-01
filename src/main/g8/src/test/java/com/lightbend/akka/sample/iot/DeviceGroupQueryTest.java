package com.lightbend.akka.sample.iot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import com.lightbend.akka.sample.iot.Device.ReadTemperature;
import com.lightbend.akka.sample.iot.DeviceGroup.*;
import org.junit.Assert;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DeviceGroupQueryTest {
    @Test
    public void testReturnTemperatureValueForWorkingDevices() {

        ActorSystem system = ActorSystem.create("test");
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requester.getRef(),
                new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());

        RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new Temperature(1.0));
        expectedTemperatures.put("device2", new Temperature(2.0));

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
        ActorSystem system = ActorSystem.create("test");
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requester.getRef(),
                new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.empty()), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());

        RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new TemperatureNotAvailable());
        expectedTemperatures.put("device2", new Temperature(2.0));

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
        ActorSystem system = ActorSystem.create("test");
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requester.getRef(),
                new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new Temperature(1.0));
        expectedTemperatures.put("device2", new DeviceNotAvailable());

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering() {
        ActorSystem system = ActorSystem.create("test");
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requester.getRef(),
                new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());
        device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

        RespondAllTemperatures response = requester.expectMsgClass(RespondAllTemperatures.class);
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new Temperature(1.0));
        expectedTemperatures.put("device2", new Temperature(2.0));

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    @Test
    public void testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime() {
        ActorSystem system = ActorSystem.create("test");
        TestKit requester = new TestKit(system);

        TestKit device1 = new TestKit(system);
        TestKit device2 = new TestKit(system);

        Map<ActorRef, String> actorToDeviceId = new HashMap<>();
        actorToDeviceId.put(device1.getRef(), "device1");
        actorToDeviceId.put(device2.getRef(), "device2");

        ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
                actorToDeviceId,
                1L,
                requester.getRef(),
                new FiniteDuration(3, TimeUnit.SECONDS)));

        assertEquals(0L, device1.expectMsgClass(ReadTemperature.class).requestId);
        assertEquals(0L, device2.expectMsgClass(ReadTemperature.class).requestId);

        queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());

        RespondAllTemperatures response = requester.expectMsgClass(
                FiniteDuration.create(5, TimeUnit.SECONDS),
                RespondAllTemperatures.class);
        assertEquals(1L, response.requestId);

        Map<String, TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new Temperature(1.0));
        expectedTemperatures.put("device2", new DeviceTimedOut());

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    //https://youtu.be/vgFoKOxrTzg
    //    https://youtu.be/uxQta776jJI
    @Test
    public void testCollectTemperaturesFromAllActiveDevices() {
        ActorSystem system = ActorSystem.create("test");
        TestKit probe = new TestKit(system);
        ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceActor1 = probe.getLastSender();

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceActor2 = probe.getLastSender();

        groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device3"), probe.getRef());
        probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
        ActorRef deviceActor3 = probe.getLastSender();

        // Check that the device actors are working
        deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), probe.getRef());
        assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
        deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), probe.getRef());
        assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).requestId);
        // No temperature for device 3

        groupActor.tell(new DeviceGroup.RequestAllTemperatures(0L), probe.getRef());
        DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
        assertEquals(0L, response.requestId);

        Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
        expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
        expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));
        expectedTemperatures.put("device3", new DeviceGroup.TemperatureNotAvailable());

        assertEqualTemperatures(expectedTemperatures, response.temperatures);
    }

    private void assertEqualTemperatures(Map<String, TemperatureReading> expectedTemperatures, Map<String, TemperatureReading> temperatures) {

        assertEquals(expectedTemperatures.size(), temperatures.size());

        expectedTemperatures.forEach((k, v) -> Assert.assertEquals(v.getValue(), temperatures.get(k).getValue(), 0.0));
    }
}