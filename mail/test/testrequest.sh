#!/bin/bash
# usage: testresponse [<test_name>]*
# executes given tests or entire request testchain
# each test is input xml file $name.xml + reference output $name.out

source testcommon.sh

cd request

  prepare_test_list $*

  if [[ $# == 0 ]]; then cleanup_failed; fi;

  PATH=../../build:..:$PATH
  run_request_tests "Blackbox request parser testing" $count "$test_list" "checkrequest"

cd ..

exit $failed
