-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V5: CREATE RISK ASSESSMENTS TABLE
-- ═══════════════════════════════════════════════════════════════════════════
--
-- RELATIONSHIP: customers (1) ─── (1) risk_assessments
-- One risk assessment profile per customer. Updated whenever the customer's
-- profile changes or a new claim is processed.
--
-- DESIGN: Why a separate table instead of columns on customers?
-- ─────────────────────────────────────────────────────────────
-- 1. Separation of concerns: Customer profile (who they are) vs
--    Risk profile (how risky they are as a policyholder)
-- 2. The risk assessment has its own lifecycle (assessed_at, reassessed)
-- 3. Future extensibility: can add ML model scores, external credit scores, etc.
-- 4. Normalized design: risk data can be versioned (track changes over time)
--
-- SCORING ALGORITHM (documented here, implemented in RiskAssessmentService):
-- ─────────────────────────────────────────────────────────────────────────
--   Age Score (0-10):
--     < 25 years      → 8  (young drivers, high accident risk)
--     25-40 years     → 3  (prime age, lowest risk)
--     40-60 years     → 6  (increasing health risks)
--     > 60 years      → 9  (highest health/life risk)
--
--   Coverage Score (0-10):
--     < 100,000       → 2  (low coverage = low exposure)
--     100k - 500k     → 5  (moderate coverage)
--     500k - 1M       → 8  (high coverage)
--     > 1M            → 10 (very high exposure)
--
--   Claim History Score (0-10):
--     0 claims        → 1  (clean history)
--     1-2 claims      → 4  (some history)
--     3-5 claims      → 7  (concerning pattern)
--     > 5 claims      → 10 (serial claimant)
--
--   Total Score = (age_score × 0.3) + (coverage_score × 0.4) + (claim_history_score × 0.3)
--   LOW:    score < 4.0
--   MEDIUM: score 4.0 - 7.0
--   HIGH:   score > 7.0
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS risk_assessments (
    -- PRIMARY KEY
    id              BIGSERIAL       PRIMARY KEY,

    -- FOREIGN KEY: One-to-One with customers
    -- UNIQUE enforces the one-to-one constraint
    customer_id     BIGINT          NOT NULL UNIQUE,
    CONSTRAINT fk_risk_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,

    -- INDIVIDUAL SCORE COMPONENTS (stored for transparency and auditability)
    -- Storing component scores allows: debugging, explaining the result to the customer,
    -- and recalculating the total if the weighting formula changes.
    age_score           SMALLINT    NOT NULL DEFAULT 0,   -- 0-10 scale
    coverage_score      SMALLINT    NOT NULL DEFAULT 0,   -- 0-10 scale
    claim_history_score SMALLINT    NOT NULL DEFAULT 0,   -- 0-10 scale

    -- WEIGHTED TOTAL SCORE
    -- NUMERIC(5,2) allows values like 7.50 (max is 10.00)
    total_risk_score    NUMERIC(5, 2) NOT NULL DEFAULT 0.00,

    -- RISK LEVEL: maps to RiskLevel enum: LOW, MEDIUM, HIGH
    risk_level          VARCHAR(10) NOT NULL DEFAULT 'LOW',

    -- ASSESSMENT METADATA
    -- When was this assessment last calculated?
    assessed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Brief explanation of the risk level (for customer transparency)
    assessment_notes    TEXT,

    -- AUDIT FIELDS
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ─── INDEXES ────────────────────────────────────────────────────────────────
-- customer_id: Primary lookup (UNIQUE already creates an index)
CREATE UNIQUE INDEX IF NOT EXISTS idx_risk_customer_id ON risk_assessments(customer_id);

-- risk_level: For admin queries ("how many HIGH risk customers do we have?")
CREATE INDEX IF NOT EXISTS idx_risk_level ON risk_assessments(risk_level);

-- ─── COMMENTS ───────────────────────────────────────────────────────────────
COMMENT ON TABLE risk_assessments IS 'Risk profile per customer. One-to-one with customers. Recalculated on profile/claim changes.';
COMMENT ON COLUMN risk_assessments.age_score IS 'Score 0-10 based on customer age. Older = higher risk.';
COMMENT ON COLUMN risk_assessments.coverage_score IS 'Score 0-10 based on total coverage amount. Higher coverage = higher risk.';
COMMENT ON COLUMN risk_assessments.claim_history_score IS 'Score 0-10 based on number of past claims. More claims = higher risk.';
COMMENT ON COLUMN risk_assessments.total_risk_score IS 'Weighted total: (age×0.3) + (coverage×0.4) + (history×0.3). Max 10.00.';
COMMENT ON COLUMN risk_assessments.risk_level IS 'LOW (<4), MEDIUM (4-7), HIGH (>7). Drives premium multiplier.';
