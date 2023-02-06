#!/bin/bash
set -e

if [[ -z "$TRENDBOX_PULL_REQUEST_NUMBER" ]]; then
    echo "Не указана переменная окружения TRENDBOX_PULL_REQUEST_NUMBER, пропускаем тестирование публикации на github.com"
    exit 0
fi

if [[ -z "$npm_package_version" ]]; then
    echo "npm_package_version env variable must be defined."
    exit 1
fi

if [[ -z "$TAP_ROBOT_EXTERNAL_GITHUB_SSH_KEY" ]]; then
    echo "TAP_ROBOT_EXTERNAL_GITHUB_SSH_KEY env variable must be defined."
    exit 1
fi

version="$npm_package_version"
echo "❯ test-external: Тестируем версию: $version"

tmp_dir=`mktemp -d -t "tap-js-api.XXXXXXXX"`
echo "❯ test-external: Временная директория: $tmp_dir"

repo_dir="${tmp_dir}/repo"
diff_dir="${tmp_dir}/diff"
diff_repo_txt="${tmp_dir}/diff/repo.txt"
diff_repo_html="${tmp_dir}/diff/repo.html"
diff_package_txt="${tmp_dir}/diff/package.txt"
diff_package_html="${tmp_dir}/diff/package.html"
key_file="${tmp_dir}/github_id_rsa"

echo "❯ test-external: Добавляем ssh ключ для авторизации на github.com из env.TAP_ROBOT_EXTERNAL_GITHUB_SSH_KEY"
cat <<< "$TAP_ROBOT_EXTERNAL_GITHUB_SSH_KEY" > $key_file
chmod 400 $key_file
ssh-add $key_file

echo "❯ test-external: Клонируем репозиторий"
git clone git@github.com:yandex/tap-js-api.git $repo_dir

echo "❯ test-external: Подготавливаем внешний репозиторий"
node external/cleanup-repo.js "$repo_dir"
node external/prepare-external-repo.js "`pwd`" "$repo_dir"

echo "❯ test-external: Проверяем ссылки на внутренние ресурсы"
node ./node_modules/@yandex-int/tap-release/cli/lint-external-repo.js "$repo_dir"

echo "❯ test-external: готовим diff с изменениями в репозитории"
mkdir -p $diff_dir
(cd $repo_dir && git add .)
(cd $repo_dir && git diff --staged > $diff_repo_txt)
if [ -s "$diff_repo_txt" ]; then
    npx diff2html --input file --style side --summary open --matching lines --output stdout -- "$diff_repo_txt" > "$diff_repo_html"
else
    echo "No changes" > $diff_repo_html
fi

echo "❯ test-external: готовим diff с изменениями в npm пакете"
(cd $repo_dir && npm pack)
package_file="yandex-tap-js-api-${version}.tgz"
mv "$repo_dir/$package_file" "$tmp_dir/$package_file"
(cd $repo_dir && git reset --hard && git clean -fxd)
(cd $repo_dir && git checkout -b build origin/build)
(cd $repo_dir && git pull --rebase origin build)
node external/cleanup-repo.js "$repo_dir"
tar xvzf "$tmp_dir/$package_file" -C "$repo_dir" --strip-components 1
node external/cleanup-package-json.js "$repo_dir"
(cd $repo_dir && git add .)
(cd $repo_dir && git diff --staged > $diff_package_txt)
if [ -s "$diff_package_txt" ]; then
    npx diff2html --input file --style side --summary open --matching lines --output stdout -- "$diff_package_txt" > "$diff_package_html"
else
    echo "No changes" > $diff_package_html
fi

echo "❯ test-external: Заливаем diff на S3"
YENV=testing DIFF_DIR="$diff_dir" npx static-uploader

echo "Удаляем $tmp_dir"
rm -rf $tmp_dir
