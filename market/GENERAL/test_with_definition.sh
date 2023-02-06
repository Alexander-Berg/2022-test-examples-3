#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"
cd "$PROJECT_DIR" || exit 1

nvm use

npm ci

if [ -n "$DEFINITIONS_SOURCE" ]; then
  curl $DEFINITIONS_SOURCE > src/java/definitions.ts
fi

npm run test
