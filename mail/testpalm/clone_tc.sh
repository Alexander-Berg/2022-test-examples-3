#!/usr/bin/env bash
base_url="https://testpalm-api.yandex-team.ru"
path="testcases/clone"
content_type="Content-Type: application/json"

if [[ "$1" = "--range" ]]
then
    shift

    if [[ "$#" -ne 5 ]]
    then
        echo "Required to pass apikey then source project name then destination project name then first case id and then last case id separated by space"
        exit 1
    fi

    auth="Authorization: OAuth $1"
    source_project="$2"
    destination_project="$3"
    current_case_id="$4"
    last_case_id="$5"
    data="["

    while [[ ${current_case_id} -le ${last_case_id} ]]
    do
        data+="$current_case_id, "
        current_case_id=$(( $current_case_id + 1 ))
    done

    len=${#data}
    data=${data:0:len-2}
    data+="]"

    curl -X POST -H "$auth" -H "$content_type" --data-binary "$data" "$base_url/$path/$source_project/$destination_project"
else

    if [[ "$#" -ne 4 ]]
    then
        echo "Not all parameters passed. Required to pass oauth token then source project name then destination project name and then all case ids separated by comma inside []"
        exit 1
    fi

    auth="Authorization: OAuth $1"
    source_project="$2"
    destination_project="$3"
    data="[$4]"
    echo ${data}

    curl -X POST -H "$auth" -H "$content_type" --data-binary "$data" "$base_url/$path/$source_project/$destination_project"
fi
