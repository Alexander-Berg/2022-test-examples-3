#!/bin/bash

source common.sh

trap '[ "$?" -eq 0 ] && echo success || echo fail' EXIT

set -ex


(
  semver_parse_into '11.22.33' X Y Z
  [ "$X" == "11" ]
  [ "$Y" == "22" ]
  [ "$Z" == "33" ]
)

(
  semver_parse_into 11.22 X Y Z
  [ "$X" == "11" ]
  [ "$Y" == "22" ]
  [ "$Z" == "" ]
)

(
  semver_parse_into 'qwe11.22.33qwe' X Y Z
  [ "$X" == "11" ]
  [ "$Y" == "22" ]
  [ "$Z" == "33" ]
)

(
  semver_parse_into 'qwe11.22qwe' X Y Z
  [ "$X" == "11" ]
  [ "$Y" == "22" ]
  [ "$Z" == "" ]
)
