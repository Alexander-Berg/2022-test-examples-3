Grants for root@localhost
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION
GRANT PROXY ON ''@'' TO 'root'@'localhost' WITH GRANT OPTION
Grants for kek@::1
GRANT INSERT, UPDATE, SELECT, CREATE USER ON * TO kek@::1 WITH GRANT OPTION
GRANT CREATE VIEW ON qdb.* TO kek@::1
Grants for kill@127.0.0.1
GRANT USAGE ON *.* TO kill@127.0.0.1 WITH GRANT OPTION
Grants for @k@106.98.53.0
GRANT LOCK TABLES, SELECT (col, cccol) ON d.* TO @k@106.98.53.0