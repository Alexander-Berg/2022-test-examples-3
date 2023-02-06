#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$ARCADIA_PATH/market/mbo/frontend/libs/scripts/ci/prepare_nvm.sh"

cd "$ARCADIA_PATH/$PROJECT_DIR" || exit 1

set -ex

nvm use

npm ci

if [ -n "$DEFINITIONS_SOURCE" ]; then
  curl $DEFINITIONS_SOURCE > src/java/definitions.ts
fi

CI=1 npm run test
