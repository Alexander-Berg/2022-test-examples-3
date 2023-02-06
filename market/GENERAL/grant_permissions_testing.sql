-- amore_test_db_read_only
GRANT SELECT ON ALL TABLES IN SCHEMA public TO amore_test_read_only_user;


-- amore_test_db_rw
GRANT SELECT ON ALL TABLES IN SCHEMA public TO amore_test_rw_user;

GRANT INSERT, UPDATE, DELETE ON autostrategies TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON autostrategies_suppliers TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON autostrategies_vendors TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON shops TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON shops_history TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON suppliers TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON vendors TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE ON vendors_history TO amore_test_rw_user;




GRANT INSERT, UPDATE, DELETE, TRUNCATE ON clean_shops_timestamp TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON clean_suppliers_timestamp TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON clean_vendors_timestamp TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON last_work_timestamp TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON last_work_timestamp_vendors TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON last_work_timestamp_suppliers TO amore_test_rw_user;

GRANT INSERT, UPDATE, DELETE, TRUNCATE ON as_tasks TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON as_tasks_suppliers TO amore_test_rw_user;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON as_tasks_vendors TO amore_test_rw_user;
