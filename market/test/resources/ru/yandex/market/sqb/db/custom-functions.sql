
---
-- Пример user-defined функции
---

create aggregate function shops_web.strsum(
  in val varchar(128),
  in flag boolean,
  inout register varchar(128),
  inout counter int
)
  returns varchar(1024)
  no sql
  language java
  external name 'CLASSPATH:ru.yandex.market.sqb.test.db.DbFunctions.strSum';

set schema public
create function json_value(json varchar(1024), format varchar(128))
  returns varchar(1024)
  no sql
  language java
  external name 'CLASSPATH:ru.yandex.market.sqb.test.db.DbFunctions.jsonValue';
