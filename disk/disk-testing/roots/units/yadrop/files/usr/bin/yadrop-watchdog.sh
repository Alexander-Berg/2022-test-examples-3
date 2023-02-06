#!/bin/sh

# Here we set what url and how we check
CHECKURL='https://localhost:443/?alive'

check() {
    CURLOPTS="--silent --insecure --max-time $1"
    # When checking the url succeeds, just exit with good code
    curl $CURLOPTS $CHECKURL 2>&1 | grep -q pong && exit 0
}

# When check succeeds the script just terminates, so describe full unsuccessful checking loop
check 2
sleep 5
check 4
sleep 10
check 10

# Otherwise restart the service and keep exit code
service yadrop restart
RESTARTRESULT=$?

# Log restart
logger -p daemon.err "yadrop restarted"

# Return the same code as service restart command did
exit $RESTARTRESULT
