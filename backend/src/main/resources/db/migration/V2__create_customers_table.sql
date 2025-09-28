-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V2: CREATE CUSTOMERS TABLE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- RELATIONSHIP: users (1) ─── (1) customers
-- One User account maps to exactly one Customer profile.
--
-- DESIGN DECISION: Why separate users and customers tables?
-- ──────────────────────────────────────────────────────────
-- Users table: authentication data (email, password, role)
-- Customers table: business domain data (phone, address, DOB, national ID)
--
-- Benefits of separation (Single Responsibility Principle):
-- 1. Security: Auth data and personal data are isolated.
--    A breach of one table doesn't expose the other.
-- 2. Flexibility: Admin/Agent users don't need customer profile data.
-- 3. Scalability: Can introduce OAuth/SSO without changing customer schema.
--
-- JPA Mapping: @OneToOne with @JoinColumn on the customers side.
-- The FK (user_id) lives in customers, not users — because not every
-- user necessarily has a customer profile (e.g., admin users).
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS customers (
    -- PRIMARY KEY
    id              BIGSERIAL       PRIMARY KEY,

    -- FOREIGN KEY to users table (One-to-One relationship)
    -- UNIQUE constraint enforces the "one user = one customer" rule
    -- ON DELETE CASCADE: if the user is deleted, customer profile is also deleted
    -- INTERVIEW TIP: Always think about cascading behavior when modeling FK relationships.
    user_id         BIGINT          NOT NULL UNIQUE,
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- CONTACT INFORMATION
    phone_number    VARCHAR(20),
    -- address as a single text field for simplicity
    -- In a real system, consider splitting into: street, city, state, postal_code, country
    address         TEXT,

    -- DEMOGRAPHIC DATA (used for risk assessment)
    -- DATE type (not TIMESTAMP) since we only need the date, not time
    date_of_birth   DATE,

    -- National identifier (e.g., Aadhaar, SSN, NIN) — unique per customer
    -- Nullable: some customers may not provide this
    national_id     VARCHAR(50)     UNIQUE,

    -- EMERGENCY CONTACT
    emergency_contact_name  VARCHAR(200),
    emergency_contact_phone VARCHAR(20),

    -- OCCUPATION (affects risk scoring in real insurance systems)
    occupation      VARCHAR(100),

    -- AUDIT FIELDS
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── INDEXES ────────────────────────────────────────────────────────────────
-- FK columns should always be indexed to avoid full-table scans on JOINs
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);

-- national_id used for KYC lookups
CREATE INDEX IF NOT EXISTS idx_customers_national_id ON customers(national_id);

-- phone_number used for customer search
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone_number);

-- ─── COMMENTS ───────────────────────────────────────────────────────────────
COMMENT ON TABLE customers IS 'Customer profile data. One-to-one with users table. Only CUSTOMER-role users have a profile here.';
COMMENT ON COLUMN customers.user_id IS 'FK to users.id. UNIQUE ensures one-to-one mapping.';
COMMENT ON COLUMN customers.date_of_birth IS 'Used in risk assessment age scoring.';
COMMENT ON COLUMN customers.national_id IS 'Government-issued ID for KYC verification. Nullable.';
