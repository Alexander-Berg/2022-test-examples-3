#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

npm ci
npx lerna run build
npx lerna run test

