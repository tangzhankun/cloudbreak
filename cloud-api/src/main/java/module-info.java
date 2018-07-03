module cloud.api {
    requires guava;
    requires jackson.annotations;
    requires spring.boot;
    requires spring.context;
    requires cloud.common;
    requires core.api;
    requires core;
    exports com.sequenceiq.cloudbreak.cloud;
    exports com.sequenceiq.cloudbreak.cloud.context;
    exports com.sequenceiq.cloudbreak.cloud.credential;
    exports com.sequenceiq.cloudbreak.cloud.exception;
    exports com.sequenceiq.cloudbreak.cloud.model;
    exports com.sequenceiq.cloudbreak.cloud.model.catalog;
    exports com.sequenceiq.cloudbreak.cloud.model.component;
    exports com.sequenceiq.cloudbreak.cloud.model.filesystem;
    exports com.sequenceiq.cloudbreak.cloud.model.generic;
    exports com.sequenceiq.cloudbreak.cloud.model.view;
    exports com.sequenceiq.cloudbreak.cloud.notification;
    exports com.sequenceiq.cloudbreak.cloud.notification.model;
}