#!/bin/bash

STATUS=0

npm run test:client:unit
[[ "$?" != 0 ]] && STATUS=1

npm run yt-reporter -- --token=${YT_TOKEN} --config='yt-test-reporter.unit.config.js'

npm run test:client:integration
[[ "$?" != 0 ]] && STATUS=1

npm run yt-reporter -- --token=${YT_TOKEN} --config='yt-test-reporter.integration.config.js'

cd server && npm ci && cd ..

npm run test:server:unit
[[ "$?" != 0 ]] && STATUS=1

exit $STATUS
