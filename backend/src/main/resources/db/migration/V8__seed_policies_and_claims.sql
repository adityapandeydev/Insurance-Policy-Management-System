-- Insert policies for Customer 6
INSERT INTO policies (
    policy_number, policy_name, policy_type, description, 
    coverage_amount, premium_amount, start_date, end_date, 
    status, premium_frequency, customer_id, created_at, updated_at
) VALUES (
    'POL-20240101-99901', 'Comprehensive Auto Secure', 'VEHICLE', 'Full coverage for your primary vehicle including collision and comprehensive.',
    50000.00, 150.00, '2024-01-01', '2025-01-01',
    'ACTIVE', 'MONTHLY', 6, NOW(), NOW()
), (
    'POL-20240215-99902', 'Family Health Plus', 'HEALTH', 'Comprehensive health insurance covering standard medical procedures and emergencies.',
    250000.00, 450.00, '2024-02-15', '2025-02-15',
    'ACTIVE', 'MONTHLY', 6, NOW(), NOW()
);

-- Insert claims for Customer 6 (using the policy_id of the inserted policies)
-- Note: Assuming the policy IDs generated are sequential, but safer to use a subquery.
-- Since this is a test seed, we can use a subquery to find the policy id.

INSERT INTO claims (
    claim_number, description, claim_amount, status, incident_date, policy_id, customer_id, created_at, updated_at
) 
SELECT 
    'CLM-20240510-88801', 'Minor fender bender in parking lot. Scratches on front bumper.', 850.00, 'APPROVED', '2024-05-09', id, 6, NOW(), NOW()
FROM policies WHERE policy_number = 'POL-20240101-99901';

INSERT INTO claims (
    claim_number, description, claim_amount, status, incident_date, policy_id, customer_id, created_at, updated_at
) 
SELECT 
    'CLM-20240601-88802', 'Emergency room visit for severe allergic reaction.', 1200.00, 'UNDER_REVIEW', '2024-05-30', id, 6, NOW(), NOW()
FROM policies WHERE policy_number = 'POL-20240215-99902';
