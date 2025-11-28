-- ═══════════════════════════════════════════════════════════════════════════
-- FLYWAY MIGRATION V9: ADD AGENT TO CUSTOMERS
-- ═══════════════════════════════════════════════════════════════════════════

-- Add agent_id to customers table
ALTER TABLE customers ADD COLUMN agent_id BIGINT;
ALTER TABLE customers ADD CONSTRAINT fk_customers_agent FOREIGN KEY (agent_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_customers_agent_id ON customers(agent_id);

-- Assign all existing customers to the default Agent (id = 2) from V6
UPDATE customers SET agent_id = 2 WHERE agent_id IS NULL;
