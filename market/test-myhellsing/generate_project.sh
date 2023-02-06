#!/bin/bash

#-a|--arcadia       path to arcadia root
#-i|--idea-project  path to idea project dir

MJVERSION=$(grep "SET(MJ_VERSION" ya.make | cut -c 16-17)
SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )


POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"

  case $key in
    -a)
      ARCADIA="$2"
      shift
      shift
      ;;
    *)
      POSITIONAL+=("$1")
      shift
      ;;
  esac
done
set -- "${POSITIONAL[@]}"

if [ -z "$ARCADIA" ]
  then
    ARCADIA="${SCRIPTPATH%/arcadia*}/arcadia"
fi

echo MJ version: ${MJVERSION}
echo -e "${ARCADIA}/market/infra/java-application/mj/${MJVERSION}/generate-project-tool/generate_project.sh $@ -p ${SCRIPTPATH} -a ${ARCADIA}\n"
${ARCADIA}/market/infra/java-application/mj/${MJVERSION}/generate-project-tool/generate_project.sh "$@" -p ${SCRIPTPATH} -a ${ARCADIA}
