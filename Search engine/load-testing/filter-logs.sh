#!/bin/sh

set -u
set -e

if [ $# -lt 1 ]; then
	echo "Usage: $0 <log_dir> [<filter>]"
	exit 1 
fi

logs_dir="${1}"

if [ $# -gt 1 ]; then
	filter="${2}"
	paste -d '\n' "${logs_dir}"/*/* | awk -f "${filter}"
else
	paste -d '\n' "${logs_dir}"/*/*
fi
