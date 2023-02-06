# Common utilities for libBlackbox testchains

# Get test list and test count
function prepare_test_list () {
# get test list
    if [[ $# > 0 ]]; then
        test_list=$*
    else
# run all tests in the directory
        test_list=`ls | grep "\.xml" | sed 's/\.xml//'` 
    fi;

    count=`echo $test_list | wc -w`
}

# Clean up failure logs
function cleanup_failed () {
    echo " (cleaning up failure logs)"
    rm -f ../failed/*.diff 
}

function print_start_line () {
    echo " ===== $1 started: $2 tests found. ===== "
}

function print_end_line () {
    echo " ===== $1 complete: run $2 tests, $3 failed ===== "
}

function print_current_line () {
    printf "%4d Running: %-40s" $1 "$2..."
}

function print_test_passed () {
    # remove successfull (empty) diff 
    rm -f ../failed/$1.diff
    echo "passed.";
}


# Run request tests, params: message, test count, test list, test command
function run_request_tests () {

    local message=$1 count=$2 test_list=$3 command=$4
    local i=1
    failed=0

    print_start_line "$message" "$count"

    for current in $test_list;
    do
        print_current_line $i $current

        $command $current.xml >../failed/$current.diff 2>&1
        if (( $? )) ; then
            echo "FAILED!";
            let failed++
        else print_test_passed $current; fi
        let i++
    done

    print_end_line "$message" $count $failed
}

# Run response tests, params: message, test count, test list, test command
function run_response_tests () {

    local message=$1 count=$2 test_list=$3 command=$4
    local i=1 type
    failed=0

    print_start_line "$message" "$count"

    for current in $test_list;
    do
        print_current_line $i $current

        # determine request type by the test name
        # if name is log* then it is login, if sess* it is session, otherwise plain response
        if echo $current | grep "^log" >/dev/null; then type="login";
        elif echo $current | grep "^sess" >/dev/null; then type="session";
        elif echo $current | grep "^multisess" >/dev/null; then type="multisession";
        elif echo $current | grep "^oauth" >/dev/null; then type="session";
        elif echo $current | grep "^host" >/dev/null; then type="host";
        elif echo $current | grep "^bulk" >/dev/null; then type="bulk";
        elif echo $current | grep "^pwdquality" >/dev/null; then type="pwdquality";
        else type="resp";
        fi

        $command $type $current.xml 2>&1 | diff - $current.out >../failed/$current.diff 2>&1
        if (( $? )) ; then
            # try to guess the error type..
            if grep "No such file or directory" ../failed/$current.diff >/dev/null; 
            then echo "Reference log missing!";
            else echo "FAILED!";
            let failed++
            fi;
        else print_test_passed $current; fi
        let i++
    done

    print_end_line "$message" $count $failed
}

