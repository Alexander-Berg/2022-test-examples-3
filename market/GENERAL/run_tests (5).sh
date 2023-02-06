#!/bin/bash -e
scriptDir="$(cd "$(dirname "$0")"; pwd)"
cd ${scriptDir}

testmodule="tests/test*.py"

if [ ! "$1" = "" ]; then
    testmodule="$1"
fi

if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Usage: $0"
    echo "Usage: $0 tests.test_data_storage"
    echo "Usage: $0 tests.test_data_storage.TestGoalsStorage.test_dump_global"
    exit 0
fi

echo "Executing the following tests:"
echo "${testmodule}"
python3 -m unittest ${testmodule}

