package com.sequenceiq.cloudbreak.core.flow2.cluster;

import com.sequenceiq.cloudbreak.core.flow2.CommonContext;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.domain.view.StackView;

public class ClusterViewContext extends CommonContext {

    private final StackView stack;

    public ClusterViewContext(String flowId, StackView stack) {
        super(flowId);
        this.stack = stack;
    }

    public StackView getStack() {
        return stack;
    }

    public ClusterView getClusterView() {
        return stack.getClusterView();
    }

    public long getStackId() {
        return stack.getId();
    }

    public long getClusterId() {
        return getClusterView().getId();
    }
}
