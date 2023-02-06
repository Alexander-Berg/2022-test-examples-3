// teardown.js

const rimraf = require('rimraf');

// eslint-disable-next-line import/no-unresolved
const {DIR} = require('./config');

module.exports = async () => {
    // close the browser instance
    await global.__BROWSER_GLOBAL__.close();

    // clean-up the wsEndpoint file
    rimraf.sync(DIR);
};
