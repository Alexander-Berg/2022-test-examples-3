CREATE TABLE market_billing.client_overdraft (
  client_id int PRIMARY KEY,
  limit_ int NOT NULL,
  spent int NOT NULL,
  payment_deadline date NULL,
  update_id int NOT NULL
);