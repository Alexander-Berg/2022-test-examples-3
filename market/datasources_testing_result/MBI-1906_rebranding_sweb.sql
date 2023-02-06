 alter
 table shops_web.datasources_stop_check
rename
    to datasources_cancel_testing
/
 alter
 table shops_web.datasources_cancel_testing
   add (fatal number(1) default 0 not null)
/
 alter
 table shops_web.datasources_cancel_testing
rename
column reason
    to reason_comment
/
 alter
 table shops_web.datasources_cancel_testing
rename
column reason_type
    to reason_code
/
