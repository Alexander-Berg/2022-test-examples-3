#!/bin/sh

# shellcheck source=scripts/prepare_java.sh
. "$PROJECT_DIR/scripts/prepare_java.sh"

# shellcheck source=scripts/prepare_nvm.sh
. "$PROJECT_DIR/scripts/prepare_nvm.sh"

cd "$PROJECT_DIR" || exit 1
nvm use

# @TODO: удалить заглушку, когда настроим тесты
echo 'Dummy test successful!'

# @TODO: раскомментировать следующие строку, когда настроим тесты
# npm ci
# npm run generate
# npm run test
