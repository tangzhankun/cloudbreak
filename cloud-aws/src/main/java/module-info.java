module cloud.aws {
    requires java.compiler;
    requires javax.inject;
    requires aws.java.sdk.autoscaling;
    requires aws.java.sdk.cloudformation;
    requires aws.java.sdk.core;
    requires aws.java.sdk.ec2;
    requires aws.java.sdk.iam;
    requires aws.java.sdk.pinpoint;
    requires aws.java.sdk.sts;
    requires aws.java.sdk.transcribe;
    requires commons.lang3;
    requires commons.net;
    requires ehcache;
    requires freemarker;
    requires guava;
    requires logback.classic;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.retry;
    requires cloud.api;
    requires cloud.common;
    requires cloud.reactor.api;
    requires cloud.reactor;
    requires core.api;
    requires core;
}