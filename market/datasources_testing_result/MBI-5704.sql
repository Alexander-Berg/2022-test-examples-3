alter table shops_web.datasources_cancel_testing
rename to datasources_testing_result
/
alter table shops_web.datasources_testing_result
rename column fatal to result
/
alter table shops_web.datasources_testing_result
rename column reason_code to result_type
/