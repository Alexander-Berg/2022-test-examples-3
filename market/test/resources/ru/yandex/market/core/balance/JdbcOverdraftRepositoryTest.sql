SET MODE oracle;
CREATE SCHEMA market_billing;
RUNSCRIPT FROM 'classpath:ru/yandex/market/core/balance/client_overdraft.tbl.tst.sql';