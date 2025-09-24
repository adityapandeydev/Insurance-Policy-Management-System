-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V6: SEED DATA
-- ═══════════════════════════════════════════════════════════════════════════
--
-- PURPOSE: Populate the database with realistic test data for:
-- 1. Development & testing: work with real data immediately
-- 2. Swagger demo: show working API responses
-- 3. Interview demo: demonstrate the system with realistic scenarios
--
-- INTERVIEW TIP: Seed data strategy
-- ─────────────────────────────────
-- Use V__ (versioned) migration for seed data that is always required.
-- Use R__ (repeatable) migrations for reference data that can be refreshed.
-- NEVER seed data in a production migration (use a separate profile/script).
-- In this case, the seed data is for DEVELOPMENT purposes only.
--
-- BCrypt hash for "Password123!" (using BCrypt strength=10):
-- $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- All demo users use this password for simplicity.
-- ═══════════════════════════════════════════════════════════════════════════

-- ─── USERS ──────────────────────────────────────────────────────────────────
-- INTERVIEW TIP: We use INSERT ... ON CONFLICT DO NOTHING to make migrations
-- idempotent (safe to run multiple times without duplicating data).
-- This is important for development when the DB might already have data.

INSERT INTO users (email, password, first_name, last_name, role, enabled)
VALUES
    -- System Administrator
    ('admin@insurance.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'System', 'Administrator', 'ROLE_ADMIN', true),

    -- Insurance Agents
    ('agent.sarah@insurance.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Sarah', 'Johnson', 'ROLE_AGENT', true),

    ('agent.mike@insurance.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Michael', 'Williams', 'ROLE_AGENT', true),

    -- Customers
    ('john.doe@email.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'John', 'Doe', 'ROLE_CUSTOMER', true),

    ('jane.smith@email.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Jane', 'Smith', 'ROLE_CUSTOMER', true),

    ('robert.brown@email.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Robert', 'Brown', 'ROLE_CUSTOMER', true),

    ('emily.davis@email.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Emily', 'Davis', 'ROLE_CUSTOMER', true),

    ('michael.wilson@email.com',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'Michael', 'Wilson', 'ROLE_CUSTOMER', true)

ON CONFLICT (email) DO NOTHING;

-- ─── CUSTOMER PROFILES ──────────────────────────────────────────────────────
-- Using subquery to get user IDs by email (avoids hardcoded IDs that may vary)
INSERT INTO customers (user_id, phone_number, address, date_of_birth, national_id, occupation)
VALUES
    ((SELECT id FROM users WHERE email = 'john.doe@email.com'),
     '+91-9876543210', '123 Main Street, Mumbai, Maharashtra 400001',
     '1990-05-15', 'AADH123456789', 'Software Engineer'),

    ((SELECT id FROM users WHERE email = 'jane.smith@email.com'),
     '+91-9876543211', '456 Park Avenue, Delhi, Delhi 110001',
     '1985-08-22', 'AADH234567890', 'Doctor'),

    ((SELECT id FROM users WHERE email = 'robert.brown@email.com'),
     '+91-9876543212', '789 Oak Road, Bangalore, Karnataka 560001',
     '1975-12-10', 'AADH345678901', 'Business Owner'),

    ((SELECT id FROM users WHERE email = 'emily.davis@email.com'),
     '+91-9876543213', '321 Elm Street, Chennai, Tamil Nadu 600001',
     '1995-03-28', 'AADH456789012', 'Teacher'),

    ((SELECT id FROM users WHERE email = 'michael.wilson@email.com'),
     '+91-9876543214', '654 Pine Lane, Hyderabad, Telangana 500001',
     '1965-11-05', 'AADH567890123', 'Retired')

ON CONFLICT (user_id) DO NOTHING;

-- ─── POLICIES ───────────────────────────────────────────────────────────────
INSERT INTO policies (
    policy_number, policy_name, policy_type, description,
    coverage_amount, premium_amount, start_date, end_date,
    status, premium_frequency, customer_id
)
VALUES
    -- John Doe's policies
    ('POL-20240101-00001', 'John Life Protect Plus', 'LIFE',
     'Comprehensive life insurance policy with accidental death benefit',
     5000000.00, 2083.33, '2024-01-01', '2034-01-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'john.doe@email.com'))),

    ('POL-20240201-00002', 'John Health Shield', 'HEALTH',
     'Family floater health insurance covering hospitalization and critical illness',
     500000.00, 4166.67, '2024-02-01', '2025-02-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'john.doe@email.com'))),

    -- Jane Smith's policies
    ('POL-20240115-00003', 'Jane Medical Pro', 'HEALTH',
     'Premium health insurance for healthcare professionals',
     1000000.00, 6250.00, '2024-01-15', '2025-01-15', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'jane.smith@email.com'))),

    ('POL-20230601-00004', 'Jane Life Shield', 'LIFE',
     'Term life insurance for 10 years',
     2000000.00, 833.33, '2023-06-01', '2033-06-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'jane.smith@email.com'))),

    -- Robert Brown's policies
    ('POL-20240301-00005', 'Robert Business Asset Cover', 'PROPERTY',
     'Commercial property insurance for business premises',
     10000000.00, 6666.67, '2024-03-01', '2025-03-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'robert.brown@email.com'))),

    ('POL-20231201-00006', 'Robert Vehicle Comprehensive', 'VEHICLE',
     'Comprehensive vehicle insurance for BMW 5 Series',
     2500000.00, 4166.67, '2023-12-01', '2024-12-01', 'EXPIRED', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'robert.brown@email.com'))),

    -- Emily Davis's policies
    ('POL-20240401-00007', 'Emily Teachers Health Plan', 'HEALTH',
     'Health insurance plan for educators',
     300000.00, 2500.00, '2024-04-01', '2025-04-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'emily.davis@email.com'))),

    -- Michael Wilson's policies (elderly customer - higher risk)
    ('POL-20240101-00008', 'Michael Senior Life Secure', 'LIFE',
     'Senior citizen life insurance with critical illness cover',
     3000000.00, 12500.00, '2024-01-01', '2034-01-01', 'ACTIVE', 'MONTHLY',
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'michael.wilson@email.com')))

ON CONFLICT (policy_number) DO NOTHING;

-- ─── CLAIMS ─────────────────────────────────────────────────────────────────
INSERT INTO claims (
    claim_number, description, claim_amount, status,
    review_notes, submitted_at, reviewed_at, incident_date,
    policy_id, customer_id
)
VALUES
    -- John's approved health claim
    ('CLM-20240510-00001',
     'Hospitalization for appendectomy surgery at Lilavati Hospital Mumbai',
     85000.00, 'APPROVED',
     'Claim approved. All medical documents verified. Hospital bills legitimate.',
     '2024-05-10 10:30:00+05:30', '2024-05-12 14:00:00+05:30', '2024-05-08',
     (SELECT id FROM policies WHERE policy_number = 'POL-20240201-00002'),
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'john.doe@email.com'))),

    -- Jane's pending health claim
    ('CLM-20240605-00002',
     'Emergency appendix removal surgery at AIIMS Delhi',
     120000.00, 'PENDING',
     NULL,
     '2024-06-05 09:00:00+05:30', NULL, '2024-06-03',
     (SELECT id FROM policies WHERE policy_number = 'POL-20240115-00003'),
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'jane.smith@email.com'))),

    -- Jane's rejected claim
    ('CLM-20240301-00003',
     'Routine annual health check-up and blood tests',
     15000.00, 'REJECTED',
     'Claim rejected: Routine check-ups are excluded under the policy terms. Please review policy exclusion clause 4.2.',
     '2024-03-01 11:00:00+05:30', '2024-03-03 16:30:00+05:30', '2024-02-28',
     (SELECT id FROM policies WHERE policy_number = 'POL-20240115-00003'),
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'jane.smith@email.com'))),

    -- Robert's property claim under review
    ('CLM-20240520-00004',
     'Office fire damage - partial destruction of warehouse storage area',
     450000.00, 'UNDER_REVIEW',
     'Fire brigade report obtained. Inspection scheduled for next week.',
     '2024-05-20 08:00:00+05:30', NULL, '2024-05-18',
     (SELECT id FROM policies WHERE policy_number = 'POL-20240301-00005'),
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'robert.brown@email.com'))),

    -- Michael's life/health claim
    ('CLM-20240410-00005',
     'Cardiac bypass surgery at Narayana Health Hyderabad',
     380000.00, 'APPROVED',
     'Claim approved. Critical illness covered under policy. Full amount authorized.',
     '2024-04-10 07:30:00+05:30', '2024-04-15 12:00:00+05:30', '2024-04-08',
     (SELECT id FROM policies WHERE policy_number = 'POL-20240101-00008'),
     (SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'michael.wilson@email.com')))

ON CONFLICT (claim_number) DO NOTHING;

-- ─── RISK ASSESSMENTS ───────────────────────────────────────────────────────
INSERT INTO risk_assessments (
    customer_id, age_score, coverage_score, claim_history_score,
    total_risk_score, risk_level, assessment_notes, assessed_at
)
VALUES
    -- John Doe: Age 34, coverage 5.5M, 1 approved claim → MEDIUM risk
    ((SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'john.doe@email.com')),
     3, 8, 4, 5.30, 'MEDIUM',
     'Medium risk: moderate coverage amounts offset by clean claim history. Age factor favorable.',
     NOW()),

    -- Jane Smith: Age 39, coverage 3M, 2 claims (1 approved, 1 rejected) → MEDIUM risk
    ((SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'jane.smith@email.com')),
     3, 8, 4, 5.30, 'MEDIUM',
     'Medium risk: experienced doctor with above-average coverage. Claim history within acceptable range.',
     NOW()),

    -- Robert Brown: Age 49, coverage 12.5M, 1 claim under review → HIGH risk
    ((SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'robert.brown@email.com')),
     6, 10, 4, 7.20, 'HIGH',
     'High risk: very high coverage amount for commercial property. Age category adds moderate risk.',
     NOW()),

    -- Emily Davis: Age 29, coverage 300K, no claims → LOW risk
    ((SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'emily.davis@email.com')),
     3, 2, 1, 2.00, 'LOW',
     'Low risk: young professional with minimal coverage and clean claims history.',
     NOW()),

    -- Michael Wilson: Age 59, coverage 3M, 1 major approved claim → HIGH risk
    ((SELECT id FROM customers WHERE user_id = (SELECT id FROM users WHERE email = 'michael.wilson@email.com')),
     9, 8, 4, 7.10, 'HIGH',
     'High risk: senior citizen with significant life insurance coverage and cardiac history.',
     NOW())

ON CONFLICT (customer_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════════════
-- SEED DATA SUMMARY
-- ═══════════════════════════════════════════════════════════════════════════
-- Users:    8 (1 admin, 2 agents, 5 customers)
-- Customers: 5 profiles
-- Policies: 8 (6 ACTIVE, 1 EXPIRED, 0 CANCELLED, 1 PENDING via normal ops)
-- Claims:   5 (1 PENDING, 1 UNDER_REVIEW, 2 APPROVED, 1 REJECTED)
-- Risk:     5 assessments (2 LOW, 2 MEDIUM, 1 HIGH)
--
-- Demo Login Credentials:
-- Admin:    admin@insurance.com / Password123!
-- Agent:    agent.sarah@insurance.com / Password123!
-- Customer: john.doe@email.com / Password123!
-- ═══════════════════════════════════════════════════════════════════════════
