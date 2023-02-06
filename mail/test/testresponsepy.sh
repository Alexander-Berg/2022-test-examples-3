#!/bin/bash
# usage: testresponse [<test_name>]*
# executes given tests or entire response testchain
# each test is input xml file $name.xml + reference output $name.out

source testcommon.sh

cd response

  prepare_test_list $*

  if [[ $# == 0 ]]; then
    cleanup_failed
  fi;

  run_response_tests "Blackbox response parser testing (Python)" $count "$test_list" "../../python/parseresponse.py"

cd ..

exit $failed
