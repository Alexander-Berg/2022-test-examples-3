#!/bin/bash
set -e

for opt in "$@"; do
  case ${opt} in
    --test-branch=*) TEST_BRANCH="${opt#*=}"
    shift ;;

    --build-user=*) BUILD_USER="${opt#*=}"
    shift ;;

    --build-id=*) BUILD_ID="${opt#*=}"
    shift ;;

    *)

    ;;
  esac
done

function assert_not_empty () {
    if [ ! -n "$1" ]; then
        echo "$2 is empty"
        echo "Usage $BASH_SOURCE --test-branch=... --build-user=... --build-id=..."
        exit 1
    fi
}

assert_not_empty ${TEST_BRANCH} 'Test branch'
assert_not_empty ${BUILD_USER} 'Build user'
assert_not_empty ${BUILD_ID} 'Build id'

git branch ${TEST_BRANCH}
git checkout ${TEST_BRANCH}
git push --set-upstream origin ${TEST_BRANCH}

AGENT_FILES_PATH=$(dirname $(dirname "$BASH_SOURCE[0]"))

# detect abs path of sources
SAVED=`pwd`
cd ${AGENT_FILES_PATH}
AGENT_FILES_PATH=`pwd`
cd ${SAVED}

BUILDER_PATH="$AGENT_FILES_PATH/src/project_builder"
TEST_PROJECTS_PATH="$BUILDER_PATH/lib/tests/test_projects"

export PYTHONPATH=${PYTHONPATH}:${BUILDER_PATH}
python ${TEST_PROJECTS_PATH}/integration_tests.py \
    --builderPath "$BUILDER_PATH" \
    --buildUser "$BUILD_USER" \
    --buildId "$BUILD_ID"