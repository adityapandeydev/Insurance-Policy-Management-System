-- Assign existing customers to agent 12
DO $$ 
DECLARE
  new_user_1_id BIGINT;
  new_user_2_id BIGINT;
BEGIN
  -- Assign existing customers (including user_id = 11) to agent 12
  UPDATE customers SET agent_id = 12 WHERE user_id = 11 OR agent_id IS NULL;

  -- Insert dummy user 1
  INSERT INTO users (first_name, last_name, email, password, role, enabled, created_at, updated_at)
  VALUES ('Alice', 'Smith', 'alice@example.com', '$2a$10$dummyhash', 'ROLE_CUSTOMER', true, NOW(), NOW())
  RETURNING id INTO new_user_1_id;

  INSERT INTO customers (user_id, phone_number, address, date_of_birth, occupation, national_id, emergency_contact_name, emergency_contact_phone, agent_id, created_at, updated_at)
  VALUES (new_user_1_id, '+1-555-0100', '123 Maple St', '1985-04-12', 'Software Engineer', 'NAT-12345', 'John Smith', '+1-555-0101', 12, NOW(), NOW());

  -- Insert dummy user 2
  INSERT INTO users (first_name, last_name, email, password, role, enabled, created_at, updated_at)
  VALUES ('Bob', 'Johnson', 'bob@example.com', '$2a$10$dummyhash', 'ROLE_CUSTOMER', true, NOW(), NOW())
  RETURNING id INTO new_user_2_id;

  INSERT INTO customers (user_id, phone_number, address, date_of_birth, occupation, national_id, emergency_contact_name, emergency_contact_phone, agent_id, created_at, updated_at)
  VALUES (new_user_2_id, '+1-555-0200', '456 Oak Ave', '1990-08-22', 'Teacher', 'NAT-67890', 'Mary Johnson', '+1-555-0201', 12, NOW(), NOW());

END $$;
