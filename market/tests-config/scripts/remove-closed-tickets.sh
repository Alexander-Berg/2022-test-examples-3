#!/bin/sh
set -e

if [ "${0%/*}" != "$0" ]
then cd "${0%/*}/.."
else cd ..
fi

node scripts/remove-tickets.js

prevBranch="$(git branch --show-current ||:)"

git checkout -b "${1:?specify new branch name}"
git add *.txt
git commit -m "${1} remove closed issues"
git push -u origin "$1"

[ -n "$prevBranch" ] && git checkout "${prevBranch:-master}"
