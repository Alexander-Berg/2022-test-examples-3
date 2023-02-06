#!/bin/bash

DB="tours"
TABLES="custom_regions"
COMMAND="mysqldump -h vertis-master-nonrt-stable.mysql.yandex.net -u tours '-p]JsFTlv9j_SvxUaE\$G(q' --single-transaction --skip-add-locks $DB $TABLES"

echo "Executing $COMMAND"
ssh d2p.vs.yandex.net "$COMMAND" | mysql -h vertis-master-nonrt-testing.mysql.yandex.net -u tours -phie2Eg1w tours