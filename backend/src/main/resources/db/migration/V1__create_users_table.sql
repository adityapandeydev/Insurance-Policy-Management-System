-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V1: CREATE USERS TABLE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- INTERVIEW TIP: Flyway Migration Naming Convention
-- ─────────────────────────────────────────────────
-- Format: V{version}__{description}.sql (double underscore)
-- V1__create_users_table.sql → applied first (lowest version number)
-- V2__create_customers_table.sql → applied second
-- ...and so on.
--
-- Flyway tracks applied migrations in the `flyway_schema_history` table.
-- Once applied, migration files must NEVER be modified (Flyway validates checksum).
-- To fix a mistake: create a new migration (V7__fix_something.sql).
--
-- DATABASE DESIGN DECISIONS:
-- • Using BIGSERIAL (auto-increment BIGINT) instead of SERIAL (INT) for IDs.
--   Reason: Insurance systems can grow to millions of records. BIGINT prevents
--   integer overflow (max 2^63 vs 2^31 for INT).
-- • Using VARCHAR with constraints instead of TEXT for indexed columns.
--   Reason: PostgreSQL can index VARCHAR efficiently; TEXT has no size limit.
-- • Using TIMESTAMPTZ (timestamp with time zone) for audit columns.
--   Reason: Always store time in UTC and let the application handle timezone
--   conversion. This prevents bugs when servers are in different timezones.
-- ═══════════════════════════════════════════════════════════════════════════

-- Enable uuid-ossp extension if we need UUID generation (future use)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    -- PRIMARY KEY
    -- BIGSERIAL = BIGINT + auto-increment sequence (PostgreSQL native)
    -- Maps to: @Id @GeneratedValue(strategy = GenerationType.IDENTITY) in JPA
    id          BIGSERIAL       PRIMARY KEY,

    -- AUTHENTICATION FIELDS
    -- UNIQUE constraint + NOT NULL enforces business rule: each email = one account
    -- INDEX: PostgreSQL auto-creates an index for UNIQUE columns (used for login lookup)
    email       VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,   -- BCrypt hash (~60 chars), never plaintext

    -- PROFILE FIELDS
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,

    -- ROLE: Stored as VARCHAR matching enum values: 'ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER'
    -- Why VARCHAR instead of PostgreSQL ENUM type?
    -- PostgreSQL ENUMs are harder to alter (require ALTER TYPE); VARCHAR is more flexible.
    role        VARCHAR(30)     NOT NULL DEFAULT 'ROLE_CUSTOMER',

    -- ACCOUNT STATUS
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,

    -- AUDIT FIELDS (populated by JPA @CreatedDate / @LastModifiedDate + AuditingEntityListener)
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── INDEXES ────────────────────────────────────────────────────────────────
-- INTERVIEW TIP: When to create indexes?
-- • Columns used in WHERE clauses frequently (email for login)
-- • Foreign key columns (prevent full table scans on JOINs)
-- • Columns used in ORDER BY on large tables
-- Rule of thumb: Index read-heavy columns; avoid over-indexing write-heavy tables
-- (each index slows INSERT/UPDATE slightly).

-- Index on email (most common lookup: login by email)
-- Note: UNIQUE constraint already creates an index; this is for documentation clarity.
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Index on role (for admin queries like "list all customers")
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- ─── COMMENTS ───────────────────────────────────────────────────────────────
COMMENT ON TABLE users IS 'Authentication and authorization table. One row per registered user.';
COMMENT ON COLUMN users.email IS 'Unique email address used for login. Must be lowercase.';
COMMENT ON COLUMN users.password IS 'BCrypt-hashed password. Never stored in plaintext.';
COMMENT ON COLUMN users.role IS 'User role: ROLE_ADMIN, ROLE_AGENT, or ROLE_CUSTOMER.';
COMMENT ON COLUMN users.enabled IS 'FALSE = soft-deleted or suspended account. Not physically deleted.';
