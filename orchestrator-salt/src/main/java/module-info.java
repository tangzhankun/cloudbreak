module orchestrator.salt {
    requires java.ws.rs;
    requires annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires commons.io;
    requires guava;
    requires jackson.annotations;
    requires jersey.client;
    requires jersey.media.multipart;
    requires logback.classic;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires cloud.common;
    requires core.api;
    requires core;
    requires orchestrator.api;
    exports com.sequenceiq.cloudbreak.orchestrator.salt.domain;
}