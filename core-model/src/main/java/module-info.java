module core.model {
    requires annotations;
    requires com.fasterxml.jackson.core;
    requires commons.codec;
    requires commons.lang3;
    requires hibernate.core;
    requires hibernate.jpa;
    requires jackson.annotations;
    requires jasypt;
    requires jasypt.hibernate4;
    requires cloud.common;
    requires core.api;
    requires core;
    exports com.sequenceiq.cloudbreak.domain;
    exports com.sequenceiq.cloudbreak.domain.json;
    exports com.sequenceiq.cloudbreak.domain.stack;
    exports com.sequenceiq.cloudbreak.domain.stack.cluster;
    exports com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;
    exports com.sequenceiq.cloudbreak.domain.stack.cluster.host;
    exports com.sequenceiq.cloudbreak.domain.stack.instance;
    exports com.sequenceiq.cloudbreak.domain.view;
}