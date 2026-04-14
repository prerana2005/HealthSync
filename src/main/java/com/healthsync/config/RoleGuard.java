package com.healthsync.config;

import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Map;

/**
 * RoleGuard — simple role-based access utility for REST endpoints.
 *
 * The frontend sends the logged-in user's role in the "X-User-Role" header
 * with every request. Endpoints that should be restricted call
 * {@link #requireRole(String, String...)} at the top of the handler.
 *
 * SOLID SRP  : access-checking logic lives here, not scattered across controllers.
 * SOLID OCP  : adding a new role never requires modifying existing controllers.
 * GRASP Low Coupling: controllers depend only on this one utility, not on any
 *               session / security framework.
 *
 * NOTE: In production you would use JWT-based Spring Security.  For this
 * university project, the header approach is sufficient to demonstrate the
 * concept of server-side role enforcement.
 */
public final class RoleGuard {

    private RoleGuard() {}   // utility class — no instances

    /**
     * Returns a 403 Forbidden response if the role in the header is not one
     * of the {@code allowedRoles}, or if the header is missing entirely.
     * Returns {@code null} when the request is permitted (caller proceeds normally).
     *
     * Typical usage inside a controller method:
     * <pre>
     *   ResponseEntity<?> denied = RoleGuard.requireRole(roleHeader, "ADMIN","DOCTOR");
     *   if (denied != null) return denied;
     * </pre>
     */
    public static ResponseEntity<?> requireRole(String roleHeader, String... allowedRoles) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: no role header provided"));
        }
        boolean allowed = Arrays.asList(allowedRoles).contains(roleHeader.trim().toUpperCase());
        if (!allowed) {
            return ResponseEntity.status(403)
                    .body(Map.of("error",
                            "Access denied: role '" + roleHeader + "' cannot access this resource"));
        }
        return null; // permitted
    }
}
