package com.insurance.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                         ROLE ENUM                                   ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Defines the three user roles in the Insurance Management System.   ║
 * ║                                                                      ║
 * ║  INTERVIEW TIP: Role-Based Access Control (RBAC)                    ║
 * ║  ─────────────────────────────────────────────────────────────────  ║
 * ║  In Spring Security, roles are authorities with the "ROLE_" prefix. ║
 * ║  When you configure .hasRole("ADMIN"), Spring internally checks     ║
 * ║  for the authority "ROLE_ADMIN".                                    ║
 * ║                                                                      ║
 * ║  Spring Security provides two abstractions:                          ║
 * ║  1. GrantedAuthority → fine-grained permissions (READ, WRITE, etc.) ║
 * ║  2. Role → coarse-grained groups of permissions (prefixed ROLE_)    ║
 * ║                                                                      ║
 * ║  Role Hierarchy in this system:                                     ║
 * ║  ADMIN   → Full access to everything                                ║
 * ║  AGENT   → Manage customers, policies, review/approve claims        ║
 * ║  CUSTOMER → Read own data, submit claims for own policies           ║
 * ║                                                                      ║
 * ║  Database Storage: Stored as String ("ROLE_ADMIN") via              ║
 * ║  @Enumerated(EnumType.STRING) — human-readable and rename-safe.     ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public enum Role {

    /**
     * ADMIN: System administrator with unrestricted access.
     * Can: manage all users, view dashboard analytics, approve/reject claims,
     *       create/delete policies, run risk assessments.
     */
    ROLE_ADMIN,

    /**
     * AGENT: Insurance agent with operational access.
     * Can: manage customers, create/update policies, review claims,
     *       approve/reject claims, view risk assessments.
     * Cannot: access system-level dashboard, delete customers permanently.
     */
    ROLE_AGENT,

    /**
     * CUSTOMER: End-user (policyholder) with self-service access.
     * Can: view own profile, view own policies, submit claims for own policies,
     *       view own claim history.
     * Cannot: view other customers, approve claims, access dashboard.
     */
    ROLE_CUSTOMER
}
