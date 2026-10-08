package dev.relay.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkspaceSlugConflictException extends RuntimeException {
    public WorkspaceSlugConflictException() {
        super("Workspace slug is already in use");
    }
}
