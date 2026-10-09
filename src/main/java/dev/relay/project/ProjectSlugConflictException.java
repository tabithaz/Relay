package dev.relay.project;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProjectSlugConflictException extends RuntimeException {
    public ProjectSlugConflictException() {
        super("Project slug is already in use in this workspace");
    }
}
