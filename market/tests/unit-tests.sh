#!/bin/bash
set -e
AGENT_FILES_PATH=$(dirname $(dirname "$BASH_SOURCE[0]"))

TC_BUILDER_FOLDER=${AGENT_FILES_PATH}/src/project_builder
pip install -r ${TC_BUILDER_FOLDER}/requirement.txt
python -m unittest discover -s ${TC_BUILDER_FOLDER}/lib -t ${TC_BUILDER_FOLDER} -p 'test_*.py'