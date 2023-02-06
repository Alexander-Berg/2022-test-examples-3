#!/bin/bash

source common.sh

trap '[ "$?" -eq 0 ] && echo success || echo fail' EXIT


[ "$(parse_startrek_tickets_from_comment "")" == "" ]

[ "$(parse_startrek_tickets_from_comment "
")" == "" ]

[ "$(parse_startrek_tickets_from_comment " ")" == "" ]

[ "$(parse_startrek_tickets_from_comment "A-1")" == "A-1" ]

[ "$(parse_startrek_tickets_from_comment "B-2 qwerty")" == "B-2" ]

[ "$(parse_startrek_tickets_from_comment "C-3 D-4")" == "C-3" ]

[ "$(parse_startrek_tickets_from_comment "D-4
E-5")" == "D-4,E-5" ]

[ "$(parse_startrek_tickets_from_comment "FFF-666 qsfad124
GGG-777 sdgsdg")" == "FFF-666,GGG-777" ]
