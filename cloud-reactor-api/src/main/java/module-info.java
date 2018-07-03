module cloud.reactor.api {
    requires guava;
    requires reactive.streams;
    requires reactor.stream;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor;
    requires core.api;
    requires core;
    exports com.sequenceiq.cloudbreak.cloud;
    exports com.sequenceiq.cloudbreak.cloud.event;
    exports com.sequenceiq.cloudbreak.cloud.event.credential;
    exports com.sequenceiq.cloudbreak.cloud.event.instance;
    exports com.sequenceiq.cloudbreak.cloud.event.model;
    exports com.sequenceiq.cloudbreak.cloud.event.platform;
    exports com.sequenceiq.cloudbreak.cloud.event.resource;
    exports com.sequenceiq.cloudbreak.cloud.event.setup;
    exports com.sequenceiq.cloudbreak.cloud.event.validation;
    exports com.sequenceiq.cloudbreak.cloud.notification.model;
    exports com.sequenceiq.cloudbreak.cloud.service;
    exports com.sequenceiq.cloudbreak.cloud.task;
}