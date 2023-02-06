--liquibase formatted sql
--changeset gaklimov:MBI-26810 endDelimiter:/
create or replace trigger shops_web.tst_datafeed_dontdelete
  before delete
  on shops_web.datafeed
  for each row
  begin
    if :old.id = 1069
    then
      raise_application_error(-20001, 'don''t delete datafeed 1069');
    end if;
  end;
/
