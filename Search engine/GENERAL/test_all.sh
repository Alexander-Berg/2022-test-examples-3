#!/usr/bin/env bash
set -e

files="$( find . -name *.json )"
for f in $files; do
    # echo $f
    python3 ./test_expressions.py "$f"
done

# autoformat web files
files="$( find web -name *.json )"
for f in $files; do
    python ./reformat.py "$f"
done
