#!/bin/bash

PREVIOUS_VALUE_FILE="/tmp/free_space_trend_avail.prev"
# Время, за которое считать приращение места. В секундах.
check_interval=${1:-300}
# Количество интервалов, длины check_interval, через которое проверять, что место кончится.
prediction_intervals=${2:-24}
# После скольки процентов свободного места считать, что оно кончилось
min_space_avail_perc=${3:-5}
# Пропускать ли рут
skip_root_space=${4-''}
message=""
status_code=0

while read -u 10 df_line; do
    name=$(echo $df_line | awk '{print $NF}')
    # Пропускаем root если указано
    [[ "$name" == "/" ]] && [ -n "$skip_root_space" ] && continue
    # Пропускаем контейнеры на dom0
    [[ "$name" == /var/lxc/root/* ]] && continue

    current_space_avail=$(echo $df_line | awk '{print $4}')
    if [ -f ${PREVIOUS_VALUE_FILE}_${name//\//_} ]; then
        previous_space_avail=$(cat ${PREVIOUS_VALUE_FILE}_${name//\//_})
    else
        echo -n $current_space_avail > ${PREVIOUS_VALUE_FILE}_${name//\//_}
        previous_space_avail=$current_space_avail
    fi
    total_space=$(echo $df_line | awk '{print $2}')

    # Считаем приращение за интервал предсказания
    prediction_trend=$(( (previous_space_avail - current_space_avail) * prediction_intervals ))
    # Считаем, сколько процентов свободного места останется в конце интервала предсказания
    prediction_space=$(( (current_space_avail - prediction_trend) * 100 / total_space ))

    if [ $prediction_space -lt $min_space_avail_perc ]; then
        status_code=2
        message="${message}$name free space will expire in $(( prediction_intervals * check_interval )) seconds. "
    fi

    # Если пошел новый интервал проверки - обновить сохраненное значение свободного места.
    last_file_update=$(stat -c %Y ${PREVIOUS_VALUE_FILE}_${name//\//_})
    interval_start=$(date --date="$check_interval seconds ago" +%s)
    if [ $last_file_update -lt $interval_start ]; then
        echo -n $current_space_avail > ${PREVIOUS_VALUE_FILE}_${name//\//_}
    fi
done 10< <(df -P -l -k -t ext2 -t ext3 -t ext4 -t xfs -t simfs 2>/dev/null | grep /)

if [ $status_code -ne 0 ]; then
    echo "$status_code;$message"
else
    echo "0; OK"
fi
