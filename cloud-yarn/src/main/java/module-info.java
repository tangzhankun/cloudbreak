module cloud.yarn {
    requires javax.inject;
    requires guava;
    requires jackson.annotations;
    requires jersey.client;
    requires logback.classic;
    requires spring.beans;
    requires spring.context;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor.api;
    requires cloud.reactor;
    requires core.api;
    requires core;
}