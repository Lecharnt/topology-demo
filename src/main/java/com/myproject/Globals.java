package com.myproject;

import java.util.List;

enum PolicyType {
    FW,
    IDS,
    WP,
    TM
}

enum RouterType {
    ER,
    CR,
    M
}

enum PathType {
    FW_IDS_WP(List.of(PolicyType.FW, PolicyType.IDS, PolicyType.WP), 32),
    FW_IDS(List.of(PolicyType.FW, PolicyType.IDS), 16),
    IDS_TM(List.of(PolicyType.IDS, PolicyType.TM), 8);

    private final List<PolicyType> middleboxes;
    private final int pathCount;

    PathType(List<PolicyType> middleboxes, int pathCount) {
        this.middleboxes = middleboxes;
        this.pathCount = pathCount;
    }

    public List<PolicyType> getMiddleboxes() {
        return middleboxes;
    }

    public int getPathCount() {
        return pathCount;
    }

    public static PathType fromFlowPolicy(List<PolicyType> policy) {
        for (PathType pathType : values()) {
            if (pathType.middleboxes.equals(policy)) {
                return pathType;
            }
        }
        return null;
    }
}
