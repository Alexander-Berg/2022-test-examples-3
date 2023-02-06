set -e

$ARCADIA_PATH/ya m -r --checkout $ARCADIA_PATH/balancer/serval $ARCADIA_PATH/balancer/serval/tools/loadgen $ARCADIA_PATH/search/priemka/rpslimiter/bin $ARCADIA_PATH/search/priemka/rpslimiter/tests/quota

$ARCADIA_PATH/search/priemka/rpslimiter/tests/quota/quota-test --serval $ARCADIA_PATH/balancer/serval/serval --loadgen $ARCADIA_PATH/balancer/serval/tools/loadgen/loadgen --rpslimiter $ARCADIA_PATH/search/priemka/rpslimiter/bin/rpslimiter --r-count 10 -c $ARCADIA_PATH/search/priemka/rpslimiter/tests/quota/config.yaml --s-max-rps 50000 --s-weight 0.1,10 --s-reload-time 0.0001 -t 60 $@
