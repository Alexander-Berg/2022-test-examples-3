#!/bin/bash
# Executes all available testchains for libBlackbox2

echo && echo "   Testing C++ library  " && echo

./testrequest.sh || exit $?
./testresponse.sh || exit $?

# echo && echo "   Testing python wrapper  " && echo

# ./testrequestpy.sh || exit $?
# ./testresponsepy.sh || exit $?
