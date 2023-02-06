-- these configs are used by test contexts in JUnit tests & and the testing environment
-- the same configs were manually inserted into the testing db
insert into billing_partner_configs(billing_client_id, description, comment, agreement_active, generate_transactions, export_to_yt, synchronize_agreement, created_at)
values(-10000004, 'Travelline (FAKE)', 'Unique id for tests & testing', true, true, true, false, '2020-09-18 18:07:52');
insert into billing_partner_configs(billing_client_id, description, comment, agreement_active, generate_transactions, export_to_yt, synchronize_agreement, created_at)
values(-10000005, 'BNovo (FAKE)', 'Unique id for tests & testing', true, true, true, false, '2020-09-18 18:07:52');
