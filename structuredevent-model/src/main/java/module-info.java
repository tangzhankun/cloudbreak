module structuredevent.model {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jackson.annotations;
    exports com.sequenceiq.cloudbreak.structuredevent.event;
    exports com.sequenceiq.cloudbreak.structuredevent.event.rest;
    exports com.sequenceiq.cloudbreak.structuredevent.json;
}