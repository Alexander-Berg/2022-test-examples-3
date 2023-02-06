#!/bin/bash
# usage: testresponse [<test_name>]*
# executes given tests or entire response testchain
# each test is input xml file $name.xml + reference output $name.out

source testcommon.sh

cd response

  prepare_test_list $*

  if [[ $# == 0 ]]; then cleanup_failed; fi;

  PATH=../../build:..:$PATH
  run_response_tests "Blackbox response parser testing" $count "$test_list" "parseresponse"

cd ..

exit $failed
