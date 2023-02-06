// puppeteer_environment.js
const fs = require('fs');

const {promisifyResponse} = require('@yandex-market/mandrel/lib/helpers');
const puppeteer = require('puppeteer');
const NodeEnvironment = require('jest-environment-node');

const {timeout = 30000, wsEndpointPath} = require('./config.js');

class PuppeteerEnvironment extends NodeEnvironment {
    async setup() {
        await super.setup();
        const wsEndpoint = fs.readFileSync(wsEndpointPath, 'utf8');
        if (!wsEndpoint) {
            throw new Error('wsEndpoint not found');
        }

        const browser = await puppeteer.connect({
            browserWSEndpoint: wsEndpoint,
        });
        const page = await browser.newPage();
        let uniqueId = 0;
        this.global.mockServer = {
            initialize(setBackendHandler, stopServer, hostName, defaultBackendHandler) {
                this.setBackendHandler = setBackendHandler;
                this.defaultBackendHandler = defaultBackendHandler;
                this.hostName = hostName;
                this.stop = stopServer;

                return this;
            },
            getHostName() {
                return this.hostName;
            },
            async openApp(
                url,
                backendHandler,
                {resolution = {width: 1280, height: 800}, waitUntil = 'networkidle0'} = {},
            ) {
                uniqueId++;
                const sk = String(uniqueId);

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
                this.setBackendHandler(backendHandler, this.defaultBackendHandler);

                page.setCookie(
                    {
                        name: 'Session_id',
                        value: '123',
                        domain: 'localhost',
                    },
                    {
                        name: 'X-YANDEXUID',
                        value: '123',
                        domain: 'localhost',
                    },
                );

                await page.goto(`${this.hostName}${url}`, {waitUntil});

                return page;
            },
        };
    }

    async teardown() {
        const stopServerResponse = this.global.stopServer ? this.global.stopServer() : Promise.resolve();

        await Promise.all([super.teardown(), promisifyResponse(stopServerResponse)]);
    }

    runScript(script) {
        return super.runScript(script);
    }
}

module.exports = PuppeteerEnvironment;
