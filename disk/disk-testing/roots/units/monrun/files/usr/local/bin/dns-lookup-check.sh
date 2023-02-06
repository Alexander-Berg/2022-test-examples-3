#!/bin/bash
#set -x 

HOSTLIST=$1
QUERY_TYPE=$2

get_ip () {
        # look for IP adress of specified record type and name
        local query_name rr_type result
        query_name=$1
        rr_type=$2

        z=0
        while [[ $z < 4 ]]; do
                result=($(dig +trace +short -t $rr_type $query_name | awk '$1 ~ /^A$|^CNAME$|^AAAA$/ {rr_type=$1;rr_val=$2} END{print rr_type,rr_val}'))
                z=$((z+1))

                if [ "${result[0]}" == "CNAME" ]; then
                        query_name=${result[1]}
                elif [ "${result[0]}" == "$rr_type" ]; then
                        echo "${result[1]}"; return 0;
                else
                        echo "lookup failed"; return 1;
                fi
        done

                echo "recursion limit reached"; return 1;
}

LOOKUP_FAILED=()
i=0

# walk through all hosts in config file
for hostname in `cat $HOSTLIST`; do
        i=$((i+1))
        result=$(get_ip $hostname $QUERY_TYPE) || LOOKUP_FAILED+=("$hostname")
done

# check status outpyt
if [ -z $LOOKUP_FAILED ]; then
        echo "0;Ok $i hostnames checked."; exit 0;
else
        echo "2;No $QUERY_TYPE records: ${LOOKUP_FAILED[*]}. $i hostnames checked."; exit 1;
fi
