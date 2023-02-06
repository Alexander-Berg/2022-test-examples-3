#!/bin/sh

. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

npm ci
npm run test || exit 1

