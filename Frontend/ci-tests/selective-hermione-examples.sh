#!/usr/bin/env bash
npx selective configure > selective-examples.json

SEL_SUITES_BUILD=$(npx selective transform --file=selective-examples.json --separator=' ' --target=examples)
export SEL_SUITES=$(eval echo -n $SEL_SUITES_BUILD)

npm run build-hermione-ci

SEL_SUITES_RUN=$(npx selective transform --file=selective-examples.json --separator='|' --target=blocks)

echo $SEL_SUITES_RUN

if [ -n "$SEL_SUITES_RUN" ]; then
  eval npm run hermione-examples-ci -- --grep $SEL_SUITES_RUN
  OUTPUT="$(echo $?)" 
else 
  OUTPUT=0
fi

rm selective-examples.json

exit "${OUTPUT}"
