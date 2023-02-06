#!/bin/bash
set -e

for opt in "$@"; do
  case ${opt} in
    --test-branch=*) TEST_BRANCH="${opt#*=}"
    shift ;;
    *)

    ;;
  esac
done

if [ ! -n "${TEST_BRANCH}" ]; then
    echo "Test branch is empty"
    exit 1
fi

git push origin --delete ${TEST_BRANCH}