const os = require('os');
const path = require('path');

let customConfig;

try {
    // eslint-disable-next-line camelcase,no-undef
    const currentRequire = typeof __non_webpack_require__ === 'undefined' ? require : __non_webpack_require__;
    customConfig = currentRequire(`${process.cwd()}/configs/jest/testServer/.config.json`);
} catch (e) {
    customConfig = {port: 3002};
}

const DIR = path.join(os.tmpdir(), 'jest_puppeteer_global_setup');
const wsEndpointPath = path.join(DIR, 'wsEndpoint');
const partnerPortPath = path.join(DIR, 'partnerPort.json');

module.exports = {
    ...customConfig,
    headless: true,
    DIR,
    wsEndpointPath,
    partnerPortPath,
};
