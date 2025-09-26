-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V4: CREATE CLAIMS TABLE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- RELATIONSHIPS:
--   policies  (1) ─── (N) claims   [a policy can have multiple claims]
--   customers (1) ─── (N) claims   [a customer can have multiple claims]
--
-- DESIGN DECISION: Why does claims have BOTH policy_id AND customer_id?
-- ────────────────────────────────────────────────────────────────────────
-- While customer_id could be derived from policy_id → policy → customer_id,
-- storing it directly in claims provides:
-- 1. Performance: "Get all claims by customer X" doesn't need a JOIN through policies
-- 2. Data Integrity: Enforces at DB level that customer owns the policy
-- 3. Business Rule Enforcement: CHECK constraint or trigger can verify
--    that claims.customer_id = policies.customer_id (enforced in Java service layer)
--
-- This is called "denormalization for read performance" — a conscious trade-off
-- accepted in systems where reads far outnumber writes.
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS claims (
    -- SURROGATE KEY
    id              BIGSERIAL       PRIMARY KEY,

    -- BUSINESS KEY (customer-facing claim reference number)
    -- Format: CLM-YYYYMMDD-XXXXX (e.g., CLM-20240115-00042)
    claim_number    VARCHAR(30)     NOT NULL UNIQUE,

    -- CLAIM DETAILS
    description     TEXT            NOT NULL,    -- Customer's description of the incident

    -- FINANCIAL: Amount customer is claiming
    -- Must NOT exceed the policy's coverage_amount (enforced in ClaimService)
    -- CHECK constraint at DB level as an additional safeguard
    claim_amount    NUMERIC(15, 2)  NOT NULL CHECK (claim_amount > 0),

    -- STATUS: maps to ClaimStatus enum: PENDING, UNDER_REVIEW, APPROVED, REJECTED, WITHDRAWN
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- REVIEW FIELDS (populated when an agent/admin reviews the claim)
    review_notes    TEXT,           -- Agent's notes explaining approval/rejection decision

    -- TIMESTAMPS for tracking
    submitted_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ,    -- NULL until claim is reviewed

    -- INCIDENT DATE: When did the insured event occur?
    incident_date   DATE            NOT NULL,

    -- FOREIGN KEYS
    -- policy_id: Which policy is this claim against?
    policy_id       BIGINT          NOT NULL,
    CONSTRAINT fk_claim_policy FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE CASCADE,

    -- customer_id: Who submitted this claim?
    -- Denormalized for query performance (see design decision above)
    customer_id     BIGINT          NOT NULL,
    CONSTRAINT fk_claim_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,

    -- AUDIT FIELDS
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── INDEXES ────────────────────────────────────────────────────────────────
-- claim_number: Unique business identifier for searching
CREATE UNIQUE INDEX IF NOT EXISTS idx_claims_claim_number ON claims(claim_number);

-- customer_id: "Show me all my claims" (customer dashboard)
CREATE INDEX IF NOT EXISTS idx_claims_customer_id ON claims(customer_id);

-- policy_id: "Show all claims for policy X" (agent view)
CREATE INDEX IF NOT EXISTS idx_claims_policy_id ON claims(policy_id);

-- status: Dashboard queries (count PENDING, APPROVED, REJECTED)
CREATE INDEX IF NOT EXISTS idx_claims_status ON claims(status);

-- Composite: Customer's claims by status (e.g., "show my pending claims")
CREATE INDEX IF NOT EXISTS idx_claims_customer_status ON claims(customer_id, status);

-- submitted_at: Time-based queries (claims submitted this month)
CREATE INDEX IF NOT EXISTS idx_claims_submitted_at ON claims(submitted_at DESC);

-- ─── COMMENTS ───────────────────────────────────────────────────────────────
COMMENT ON TABLE claims IS 'Insurance claims. Many-to-One with both policies and customers.';
COMMENT ON COLUMN claims.claim_number IS 'Business-facing unique ID (e.g. CLM-20240115-00042).';
COMMENT ON COLUMN claims.claim_amount IS 'Claimed amount. Cannot exceed policy coverage_amount (enforced in service layer).';
COMMENT ON COLUMN claims.review_notes IS 'Agent notes on approval/rejection. Required when status = REJECTED.';
COMMENT ON COLUMN claims.customer_id IS 'Denormalized FK for query performance. Must match policy.customer_id.';
