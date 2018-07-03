module cloud.openstack {
    requires java.compiler;
    requires java.ws.rs;
    requires javax.inject;
    requires com.fasterxml.jackson.core;
    requires commons.codec;
    requires commons.lang3;
    requires freemarker;
    requires guava;
    requires logback.classic;
    requires openstack4j.core;
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