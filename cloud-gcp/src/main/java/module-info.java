module cloud.gcp {
    requires java.compiler;
    requires javax.inject;
    requires com.fasterxml.jackson.databind;
    requires commons.codec;
    requires commons.lang3;
    requires google.api.client;
    requires google.api.services.compute.beta.rev76;
    requires google.api.services.storage.v1.rev94;
    requires google.http.client;
    requires google.http.client.jackson2;
    requires google.oauth.client;
    requires gson;
    requires guava;
    requires jackson.annotations;
    requires logback.classic;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor.api;
    requires cloud.reactor;
    requires cloud.template;
    requires core.api;
    requires core;
}