// puppeteer_environment.js
const fs = require('fs');

const puppeteer = require('puppeteer');
const {getLocation} = require('connected-react-router');
const NodeEnvironment = require('jest-environment-node');
const Immutable = require('seamless-immutable');

const {
    setBackendHandler,
    removeBackendHandler,
    setInitialState,
    getHostName,
    startServer,
    stopServer,
    // eslint-disable-next-line import/no-unresolved
} = require('../../lib/analytics/testServer');
const {timeout = 30000, wsEndpointPath} = require('./config.js');

class PuppeteerEnvironment extends NodeEnvironment {
    async setup() {
        await super.setup();
        // get the wsEndpoint
        const wsEndpoint = fs.readFileSync(wsEndpointPath, 'utf8');
        if (!wsEndpoint) {
            throw new Error('wsEndpoint not found');
        }

        // connect to puppeteer
        const [browser] = await Promise.all([
            puppeteer.connect({
                browserWSEndpoint: wsEndpoint,
            }),
            startServer(),
        ]);
        const page = await browser.newPage();
        let uniqueId = 0;
        this.global.mockServer = {
            getHostName,
            openApp: async (
                state,
                backendHandler,
                {resolution = {width: 1280, height: 800}, waitUntil = 'networkidle0'} = {},
            ) => {
                removeBackendHandler(String(uniqueId));
                uniqueId++;
                const sk = String(uniqueId);

                setInitialState(
                    Immutable.from(state, {deep: true}).setIn(['widgets', 'currentUser', 'sk'], String(sk)),
                );

                await Promise.all([
                    page.setViewport(resolution),
                    page.setRequestInterception(true),
                    page._client.send('Animation.setPlaybackRate', {playbackRate: 30}),
                    page.setDefaultTimeout(timeout),
                ]);
                page.removeAllListeners();
                page.on('request', request => {
                    if (!request.isNavigationRequest()) {
                        request.continue();
                        return;
                    }
                    const headers = request.headers();
                    // передаем sk в запросе к странице чтобы локализовать нужный стейт на моковом сервере
                    headers.sk = sk;
                    request.continue({headers});
                });
                setBackendHandler(sk, backendHandler);
                const {pathname, search = ''} = getLocation(state);

                await page.goto(`${getHostName()}${pathname}${search}`, {waitUntil});

                return page;
            },
        };
    }

    async teardown() {
        await Promise.all([super.teardown(), stopServer()]);
    }

    runScript(script) {
        return super.runScript(script);
    }
}

module.exports = PuppeteerEnvironment;
