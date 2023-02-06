#!/bin/bash

export NODE_ENV=testing
if [ ! -f ".tvm.dev.json" ]; then
    sed -e "s/XXX/$PASKILLS_TVM_SECRET/" .tvm.dev.json.example > .tvm.dev.json
fi
npm run dev:tvmtool &
docker run -d -p 12000:12000 -e OPT_db_grants=SUPERUSER -e OPT_pgbouncer_pool_mode=transaction -e OPT_pgbouncer_min_pool_size=10 -e OPT_pre_sql='CREATE EXTENSION "uuid-ossp"' -t -i registry.yandex.net/dbaas/minipgaas
TRIES=12
npx sequelize db:migrate
while [[ $? -gt 0 ]]
do 
sleep 5
echo $(( TRIES-- ))
if [[ $TRIES -eq 0 ]] ; then
exit 1
fi
npx sequelize db:migrate
done
npm run test