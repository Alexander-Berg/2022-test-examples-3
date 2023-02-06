#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"
. "$PROJECT_DIR/scripts/ci/prepare_current_branch.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

npm ci
npm run test
