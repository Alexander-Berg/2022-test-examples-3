ALTER TABLE mock_trust_baskets
    ADD COLUMN test_context_class_name varchar(512);
ALTER TABLE mock_trust_baskets
    ADD COLUMN test_context_data bytea;
