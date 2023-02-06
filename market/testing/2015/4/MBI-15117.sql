--liquibase formatted sql
--changeset sergey-fed:MBI-15117_delegate endDelimiter:/
create or replace package mbi_core.multitesting_helper_delegate authid CURRENT_USER as
  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number);
end multitesting_helper_delegate;
/

create or replace package body mbi_core.multitesting_helper_delegate as

  function get_next_seq_value(p_seq_name varchar2) return number is
    v_next_seq_value number;
  begin
    execute immediate 'select ' || p_seq_name || '.nextval from dual' into v_next_seq_value;
    return v_next_seq_value;
  end;

  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number) is
    v_next_seq_val number;
  begin
    v_next_seq_val := get_next_seq_value(p_seq_name);

    if p_min_value > v_next_seq_val then
      execute immediate 'alter sequence ' || p_seq_name || ' increment by ' || (p_min_value - v_next_seq_val);
      v_next_seq_val := get_next_seq_value(p_seq_name);
      execute immediate 'alter sequence ' || p_seq_name || ' increment by 1';
    end if;
  end;

end multitesting_helper_delegate;

--changeset sergey-fed:MBI-15117_delegate_privs endDelimiter:;
grant execute on mbi_core.multitesting_helper_delegate to shops_web, market_billing;

--changeset sergey-fed:MBI-15117_shops_web endDelimiter:/
create or replace package shops_web.multitesting_helper as
  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number);
end multitesting_helper;
/

create or replace package body shops_web.multitesting_helper as

  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number) is
  begin
    mbi_core.multitesting_helper_delegate.ensure_seq_value(p_seq_name, p_min_value);
  end;

end multitesting_helper;

--changeset sergey-fed:MBI-15117_shops_web_privs endDelimiter:;
grant execute on shops_web.multitesting_helper to public;

--changeset sergey-fed:MBI-15117_market_billing endDelimiter:/
create or replace package market_billing.multitesting_helper as
  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number);
end multitesting_helper;
/

create or replace package body market_billing.multitesting_helper as

  procedure ensure_seq_value(p_seq_name varchar2 , p_min_value number) is
  begin
    mbi_core.multitesting_helper_delegate.ensure_seq_value(p_seq_name, p_min_value);
  end;

end multitesting_helper;

--changeset sergey-fed:MBI-15117_market_billing_privs endDelimiter:;
grant execute on market_billing.multitesting_helper to public;