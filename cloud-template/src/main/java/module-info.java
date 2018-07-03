module cloud.template {
    requires java.compiler;
    requires javax.inject;
    requires guava;
    requires logback.classic;
    requires spring.context;
    requires spring.core;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor.api;
    requires cloud.reactor;
    requires core.api;
    exports com.sequenceiq.cloudbreak.cloud.template;
    exports com.sequenceiq.cloudbreak.cloud.template.context;
}