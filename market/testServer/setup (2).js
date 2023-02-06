// setup.js
const fs = require('fs');

const mkdirp = require('mkdirp');
const puppeteer = require('puppeteer');

const {startServer} = require('./staticServer');

// eslint-disable-next-line import/no-unresolved
const {executablePath, headless = true, port, DIR, partnerPortPath, wsEndpointPath} = require('./config');

module.exports = async () => {
    const browser = await puppeteer.launch({
        headless,
        executablePath,
        ignoreHTTPSErrors: true,
        args: [
            '--disable-web-security',
            '--ignore-certificate-errors',
            '--hide-scrollbars',
            '--enable-font-antialiasing',
            '--force-device-scale-factor=1',
            '--high-dpi-support=1',
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-background-timer-throttling',
            '--disable-backgrounding-occluded-windows',
            '--disable-renderer-backgrounding',
            '--font-render-hinting=none',
        ],
    });
    // store the browser instance so we can teardown it later
    // this global is only available in the teardown but not in TestEnvironments
    global.__BROWSER_GLOBAL__ = browser;

    // use the file system to expose the wsEndpoint for TestEnvironments
    mkdirp.sync(DIR);
    fs.writeFileSync(partnerPortPath, JSON.stringify({[port]: true}));
    fs.writeFileSync(wsEndpointPath, browser.wsEndpoint());
    startServer();
};
