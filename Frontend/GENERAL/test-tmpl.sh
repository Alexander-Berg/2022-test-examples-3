#!/bin/sh

set -e

rm -rf src/blocks/_*

cp -r src/blocks/desktop src/blocks/_instr
npx istanbul instrument src/blocks/desktop -x "!**/*.bemhtml.js" -o src/blocks/_instr

mkdir -p src/blocks/_tests
for block in "$@"; do
    ln -s ../_instr/${block} src/blocks/_tests
done

PROJ_LEVELS=blocks/_instr TEST_LEVELS=blocks/_tests BEM_TMPL_SPECS_TIMEOUT=60000 npx enb --dir src make tmpl-specs -n

rm -rf src/blocks/_*
