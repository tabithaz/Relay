package dev.relay.workspace;

public enum WorkspaceRole {
    OWNER, ADMIN, EDITOR, VIEWER;

    public boolean canWrite() {
        return this == OWNER || this == ADMIN || this == EDITOR;
    }
}
