#!/bin/sh

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/ci/prepare_nvm.sh"
. "$PROJECT_DIR/scripts/ci/prepare_current_branch.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

npm ci

npm run download-build-artifacts:list

CI=1 npm run deploy:common-logs:testing
CI=1 npm run deploy:community-sku:testing
CI=1 npm run deploy:blue-logs:testing
CI=1 npm run deploy:white-logs:testing
CI=1 npm run deploy:blue-logs-lab:testing
CI=1 npm run deploy:msku-from-psku:testing
CI=1 npm run deploy:mapping-moderation:testing
CI=1 npm run deploy:mapping-moderation-partners:testing
CI=1 npm run deploy:psku-mapping-moderation:testing
CI=1 npm run deploy:picture-moderation:testing
CI=1 npm run deploy:categorial-classification:testing
