module cloud.reactor {
    requires java.compiler;
    requires javax.inject;
    requires guava;
    requires logback.classic;
    requires reactive.streams;
    requires reactor.bus;
    requires reactor.core;
    requires reactor.stream;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.tx;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor.api;
    requires core.api;
    requires core;
    exports com.sequenceiq.cloudbreak.cloud.credential;
    exports com.sequenceiq.cloudbreak.cloud.handler;
    exports com.sequenceiq.cloudbreak.cloud.init;
    exports com.sequenceiq.cloudbreak.cloud.notification;
    exports com.sequenceiq.cloudbreak.cloud.reactor;
    exports com.sequenceiq.cloudbreak.cloud.scheduler;
    exports com.sequenceiq.cloudbreak.cloud.service;
    exports com.sequenceiq.cloudbreak.cloud.task;
    exports com.sequenceiq.cloudbreak.cloud.transform;
}