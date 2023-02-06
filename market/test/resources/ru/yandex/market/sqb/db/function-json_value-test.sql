
select json_value('{"fullAddress":"Hello, World!"}', '$.fullAddress') as value from dual;
