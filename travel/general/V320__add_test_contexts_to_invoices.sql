ALTER TABLE invoices
    ADD COLUMN payment_test_context_class_name varchar(512);
ALTER TABLE invoices
    ADD COLUMN payment_test_context_data bytea;
