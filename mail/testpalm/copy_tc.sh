#!/usr/bin/env bash
content_type="Content-Type: application/json"
base_url="https://testpalm-api.yandex-team.ru"
url="https://testpalm.yandex-team.ru"

if [[ "$1" = "--range" ]]
then
    shift

    if [[ "$#" -ne 5 ]]
    then
        echo "Required to pass oauth token then source project name then destination project name then first case id and then last case id separated by space"
        exit 1
    fi

    auth="Authorization: OAuth $1"
    source_project="$2"
    destination_project="$3"
    current_case_id="$4"
    last_case_id="$5"

    while [[ ${current_case_id} -le ${last_case_id} ]]
    do
        test_case=$(curl -H "$auth" "$base_url/testcases/$source_project/?id=$current_case_id" | jq .[0] | jq -r 'del(.id,.bugs,.tasks,.attributes,.isAutotest, .stats)' )
        curl -H "$auth" -H "$content_type" --data-binary "$test_case" "$base_url/testcases/$destination_project"
        echo "\nСase $url/testcase/$source_project-$current_case_id is cloned to project $url/$destination_project/testcases"
        current_case_id=$(( $current_case_id + 1 ))
    done
else
    if [[ "$#" -lt 4 ]]
    then
        echo "Not all parameters passed. Required to pass oauth token then source project name then destination project name and then all case ids separated by space"
        exit 1
    fi

    auth="Authorization: OAuth $1"
    source_project="$2"
    destination_project="$3"

    for case_id in ${@:4}
    do
        test_case=$(curl -H "$auth" "$base_url/testcases/$source_project/?id=$case_id" | jq .[0] | jq -r ' del(.id,.bugs,.tasks,.attributes,.isAutotest,.stats)')
        echo $test_case
        curl -H "$auth" -H "$content_type" --data-binary "$test_case" "$base_url/testcases/$destination_project"
        echo "\nСase $url/testcase/$source_project-$case_id is cloned to project $url/$destination_project/testcases"
    done
fi
