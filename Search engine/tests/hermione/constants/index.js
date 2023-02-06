const fs = require('fs');
const path = require('path');

const cronValidateCronDefaultPayload = require('./_cronValidatePayload.json');

const ROBOT_LOGIN = fs
    .readFileSync(
        path.resolve(
            __dirname,
            '../../../../../includes/jest-config/robotLogin.txt',
        ),
    )
    .toString()
    .trim();

const ROBOT_OAUTH_TOKEN = fs
    .readFileSync(
        path.resolve(
            __dirname,
            '../../../../../includes/jest-config/robotOAuthToken.txt',
        ),
    )
    .toString()
    .trim();

const defaultAssertViewOptions = {
    allowViewportOverflow: true,
};

module.exports = {
    defaultAssertViewOptions,
    cronValidateCronDefaultPayload,
    ROBOT_LOGIN,
    ROBOT_OAUTH_TOKEN,
};
