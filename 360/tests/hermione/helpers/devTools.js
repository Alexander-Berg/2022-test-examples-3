const url = require('url');
const puppeteer = require('puppeteer-core');
const { gridUrl } = require('../../../.hermione.conf');

// класс для взаимодействия с хромом по протоколу DevTools (webdriver гермионы умеет не все, что нужно)
module.exports = class DevTools {
    static browserWSEndpoint({ sessionId, gridUrl }) {
        const host = url.parse(gridUrl).host;
        return 'ws://' + host + '/devtools/' + sessionId;
    }

    static async create(browser) {
        const browserWSEndpoint = {
            sessionId: browser.requestHandler.sessionID,
            gridUrl
        };

        const _browser = await puppeteer.connect({
            browserWSEndpoint: this.browserWSEndpoint(browserWSEndpoint),
            defaultViewport: null
        });

        return new DevTools(_browser);
    }

    constructor(browser) {
        this._browser = browser;
    }

    setCommandOnNewPage(command) {
        this._browser.on('targetcreated', (target) => {
            try {
                if (target.page()) {
                    this.sendCommand(target, command).then();
                }
            } catch (error) {
                // eslint-disable-next-line no-console
                console.error(error);
            }
        });
    }

    async sendCommand(target, command) {
        const client = await target.createCDPSession();
        await client.send('Page.enable', {});
        await client.send(...command);
    }

    async disableNetworkOnPage(pageId) {
        const _pageId = pageId.replace('CDwindow-', '');
        const pages = await this._browser.pages();
        const page = pages.find((x) => x._target._targetId === _pageId);
        const client = await page.target().createCDPSession();
        await client.send('Page.enable', {});
        await client.send('Network.enable', {});
        await client.send('Network.emulateNetworkConditions', {
            offline: true,
            latency: 0,
            downloadThroughput: -1,
            uploadThroughput: -1
        });
    }

    async enableNetworkOnPage(pageId) {
        const _pageId = pageId.replace('CDwindow-', '');
        const pages = await this._browser.pages();
        const page = pages.find((x) => x._target._targetId === _pageId);
        const client = await page.target().createCDPSession();
        await client.send('Page.enable', {});
        await client.send('Network.enable', {});
        await client.send('Network.emulateNetworkConditions', {
            offline: false,
            latency: 0,
            downloadThroughput: -1,
            uploadThroughput: -1
        });
    }
};
