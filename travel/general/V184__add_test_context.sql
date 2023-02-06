ALTER TABLE order_items
    ADD COLUMN test_context_class_name varchar(512);
ALTER TABLE order_items
    ADD COLUMN test_context_data bytea;
