#!/bin/sh
CONFIG_FILE=${CONFIG_DIR}/buildAgent.properties
export CONFIG_FILE=${CONFIG_FILE}
AGENT_NAME=crm-tests-${DEPLOY_POD_ID}

# define agent environment variables or default

SERVER_URL=https://teamcity.yandex-team.ru
AGENT_LOG=${AGENT_HOME}/logs

if [ ! -d ${CONFIG_DIR} ] ; then
    mkdir -p ${CONFIG_DIR}
    echo "serverUrl=${SERVER_URL}" > ${CONFIG_FILE}
    echo "authorizationToken=" >> ${CONFIG_FILE}
    echo "name=${AGENT_NAME}" >> ${CONFIG_FILE}
fi

# Run agent
echo "${AGENT_HOME}/bin/agent.sh start"
${AGENT_HOME}/bin/agent.sh start

# Wait for agent start to write log
while [ ! -f ${AGENT_LOG}/teamcity-agent.log ];
do
    echo -n "."
    sleep 1
done

# Link buildTmp to /tmpfs
if [[ -d "/tmpfs" ]] ; then
    rm -rf "/opt/buildagent/temp"
    ln -s "/tmpfs" "/opt/buildagent/temp"
fi

trap '$(save_build_agent_authorization_token); ${AGENT_HOME}/bin/agent.sh stop force; while ps -p $(cat $(ls -1 ${AGENT_LOG}/*.pid)) &>/dev/null; do sleep 1; done; kill %%' SIGINT SIGTERM SIGHUP

tail -qF ${AGENT_LOG}/teamcity-agent.log &
wait