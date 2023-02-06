#!/bin/bash

taskConfigPath=$1

if [[ -z "${taskConfigPath}" ]]; then
    echo "First parameter taskConfigPath is required"
    exit 1;
fi

if [[ "$RELEASE_TYPE" = "stable" ]]; then
    if [[ "$TRENDBOX_GITHUB_EVENT_TYPE" != "push" || "$TRENDBOX_BRANCH" != "master" ]]; then
        echo "'Deploy stable' was cancelled because of 2 reasons."
        echo "1. Merge not to ${SANDBOX_OAUTH_TOKEN} but expected master"
        echo "2. PR was diclined"
        exit 1;
    fi;
fi;

npm install

sudo apt-get update
sudo apt-get install -y jq

tar -C . --create --file dockerfile.tar .

# кладем именно по этому пути (пример /place/sandbox-data/tasks/3/1/719930013) см. https://st.yandex-team.ru/INFRADUTY-11419
mv dockerfile.tar ../../../

export SANDBOX_RESOURCE_ID=$(./createDockerResource.sh)
export DOCKER_TAG=$(date +%Y-%m-%d_%H-%M)

cat $taskConfigPath | envsubst > ./tmpdockerReleaseTaskConfig.json

node node_modules/.bin/sandbox create-task --input=tmpdockerReleaseTaskConfig.json --token=${SANDBOX_OAUTH_TOKEN} | node node_modules/.bin/sandbox start-task --token=${SANDBOX_OAUTH_TOKEN}

cat ./tmpdockerReleaseTaskConfig.json
