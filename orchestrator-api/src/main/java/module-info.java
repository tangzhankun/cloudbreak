module orchestrator.api {
    requires jackson.annotations;
    requires logback.classic;
    requires core.api;
    exports com.sequenceiq.cloudbreak.orchestrator;
    exports com.sequenceiq.cloudbreak.orchestrator.container;
    exports com.sequenceiq.cloudbreak.orchestrator.exception;
    exports com.sequenceiq.cloudbreak.orchestrator.executor;
    exports com.sequenceiq.cloudbreak.orchestrator.host;
    exports com.sequenceiq.cloudbreak.orchestrator.model;
    exports com.sequenceiq.cloudbreak.orchestrator.model.port;
    exports com.sequenceiq.cloudbreak.orchestrator.state;
}