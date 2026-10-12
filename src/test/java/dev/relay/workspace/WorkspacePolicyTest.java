package dev.relay.workspace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspacePolicyTest {
    @Mock WorkspaceMembershipRepository memberships;
    @InjectMocks WorkspacePolicy policy;
    private final Workspace workspace = new Workspace("Team", "team");

    @Test
    void nonMemberCannotReadOrWrite() {
        assertFalse(policy.canRead(7L, "outsider"));
        assertFalse(policy.canWrite(7L, "outsider"));
        assertFalse(policy.canManageMembers(7L, "outsider"));
    }

    @Test
    void viewerCanReadButCannotWrite() {
        when(memberships.existsByWorkspace_IdAndSubject(7L, "viewer")).thenReturn(true);
        when(memberships.findByWorkspace_IdAndSubject(7L, "viewer")).thenReturn(
                Optional.of(new WorkspaceMembership(workspace, "viewer", WorkspaceRole.VIEWER)));
        assertTrue(policy.canRead(7L, "viewer"));
        assertFalse(policy.canWrite(7L, "viewer"));
        assertFalse(policy.canManageMembers(7L, "viewer"));
    }

    @Test
    void editorCanWriteButCannotManageMembers() {
        when(memberships.findByWorkspace_IdAndSubject(7L, "editor")).thenReturn(
                Optional.of(new WorkspaceMembership(workspace, "editor", WorkspaceRole.EDITOR)));
        assertTrue(policy.canWrite(7L, "editor"));
        assertFalse(policy.canManageMembers(7L, "editor"));
    }

    @Test
    void ownerCanManageMembers() {
        when(memberships.findByWorkspace_IdAndSubject(7L, "owner")).thenReturn(
                Optional.of(new WorkspaceMembership(workspace, "owner", WorkspaceRole.OWNER)));
        assertTrue(policy.canWrite(7L, "owner"));
        assertTrue(policy.canManageMembers(7L, "owner"));
    }

    @Test
    void missingIdentityFailsClosed() {
        assertFalse(policy.canRead(7L, null));
        assertFalse(policy.canWrite(7L, null));
        assertFalse(policy.canManageMembers(null, "owner"));
        verifyNoInteractions(memberships);
    }
}
