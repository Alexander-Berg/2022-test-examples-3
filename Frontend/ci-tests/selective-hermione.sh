#!/usr/bin/env bash
npx selective configure > selective-hermione.json

SEL_SUITES_BUILD=$(npx selective transform --file=selective-hermione.json --separator=' ' --target=build-hermione)
export SEL_SUITES=$(eval echo -n $SEL_SUITES_BUILD)

npm run build-hermione-ci

SEL_SUITES_RUN=$(npx selective transform --file=selective-hermione.json --separator='|' --target=hermione)

echo $SEL_SUITES_RUN

if [[ -n "$SEL_SUITES_RUN" ]]; then
  eval npx hermione $SEL_SUITES_RUN
  OUTPUT="$(echo $?)"
else
  OUTPUT=0 
fi

rm selective-hermione.json
exit "${OUTPUT}"
