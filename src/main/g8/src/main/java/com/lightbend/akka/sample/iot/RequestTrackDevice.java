package com.lightbend.akka.sample.iot;

public final class RequestTrackDevice {
    public final String groupId;
    public final String deviceId;

    public RequestTrackDevice(String groupId, String deviceId) {
        this.groupId = groupId;
        this.deviceId = deviceId;
    }
}
