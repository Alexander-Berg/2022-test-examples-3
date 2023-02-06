#!/bin/bash

source common.sh

trap '[ "$?" -eq 0 ] && echo success || echo fail' EXIT


[ "$(get_jenkins_build_id_from_last_log_line 'Completed Выкладка пакетов MBO в мультитестинг #42 alkedr mbo02 : ABORTED')" == '42' ]

[ "$(get_jenkins_build_status_from_last_log_line 'Completed Выкладка пакетов MBO в мультитестинг #133 alkedr mbo02 : SUCCESS')" == 'SUCCESS' ]
[ "$(get_jenkins_build_status_from_last_log_line 'Completed Выкладка пакетов MBO в мультитестинг #133 alkedr mbo02 : FAILURE')" == 'FAILURE' ]
