module autoscale.api {
    requires java.validation;
    requires java.ws.rs;
    requires javax.inject;
    requires expiringmap;
    requires jackson.annotations;
    requires jersey.proxy.client;
    requires logback.classic;
    requires swagger.annotations;
    requires cloud.common;
    requires core.api;
    exports com.sequenceiq.periscope.api.endpoint.v1;
    exports com.sequenceiq.periscope.api.endpoint.v2;
    exports com.sequenceiq.periscope.api.model;
    exports com.sequenceiq.periscope.client;
}