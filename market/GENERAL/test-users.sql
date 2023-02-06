declare
  i integer;
begin
  for i in 1..10000 loop
    insert into market_api.market_api_key_ip (secret, created) 
    values (i, sysdate);
  end loop;
end; 
