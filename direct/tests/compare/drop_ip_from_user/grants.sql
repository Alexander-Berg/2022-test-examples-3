Grants for root@localhost
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION
GRANT PROXY ON ''@'' TO 'root'@'localhost' WITH GRANT OPTION
Grants for usr@1.1.1.1
GRANT EXECUTE ON FUNCTION d.f TO usr@1.1.1.1
GRANT ALL PRIVILEGES ON * TO usr@1.1.1.1
Grants for usr@localhost
GRANT EXECUTE ON FUNCTION d.f TO usr@localhost