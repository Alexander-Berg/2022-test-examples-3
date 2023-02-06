#!/usr/bin/env node

const { expect } = require('chai');
const puppeteer = require('puppeteer');
const HOST = process.env.HOST || 'https://localhost.yandex.ru:9000';

const FILTER = [
    'https://mc.yandex.ru/metrika/tag.js',
    'https://yastatic.net/daas/atom.js',
    'https://yastatic.net/nearest.js',
];

function normalizeUrl(url) {
    return url.replace(HOST, '')
        .replace('/collections/assets/desktop/', '')
        .replace('/collections/assets/mobile/', '');
}

function filterScripts(scripts) {
    return scripts.filter((script) => !FILTER.includes(script));
}

const CASES = {
    home: {
        url: '/',
        mobile: [
            'vendor.bundle.js',
            'index.bundle.js',
            'MobileCardSeriesCtrl.hvalanka.bundle.js',
            'MobileTimelinePopupSection.hvalanka.bundle.js',
        ],
        desktop: [
            'vendor.bundle.js',
            'index.bundle.js',
            '0.bundle.js',
            'ReduxSync.dynamic.bundle.js',
            'DesktopSidebarCardDrop.hvalanka.bundle.js',
        ],
    },

    profile: {
        url: '/user/ftdebugger/',
        mobile: [
            'vendor.bundle.js',
            'index.bundle.js',
            'ProfilePage.dynamic.bundle.js',
            'MobileCardSeriesCtrl.hvalanka.bundle.js',
            'MobileTimelinePopupSection.hvalanka.bundle.js',
        ],
        desktop: [
            'vendor.bundle.js',
            'index.bundle.js',
            '0.bundle.js',
            'ProfilePage.dynamic.bundle.js',
            'ReduxSync.dynamic.bundle.js',
            'DesktopSidebarCardDrop.hvalanka.bundle.js',
        ],
    },

    profileCards: {
        url: '/user/ftdebugger/_cards/',
        mobile: [
            'vendor.bundle.js',
            'index.bundle.js',
            'ProfilePage.dynamic.bundle.js',
            'ReduxSync.dynamic.bundle.js',
            'MobileCardSeriesCtrl.hvalanka.bundle.js',
            'MobileTimelinePopupSection.hvalanka.bundle.js',
        ],
        desktop: [
            'vendor.bundle.js',
            'index.bundle.js',
            '0.bundle.js',
            'ProfilePage.dynamic.bundle.js',
            'ReduxSync.dynamic.bundle.js',
            'DesktopSidebarCardDrop.hvalanka.bundle.js',
        ],
    },

    vertical: {
        url: '/retsepty/',
        mobile: [
            'vendor.bundle.js',
            'index.bundle.js',
            'VerticalPage.dynamic.bundle.js',
            'MobileCardSeriesCtrl.hvalanka.bundle.js',
            'MobileTimelinePopupSection.hvalanka.bundle.js',
        ],
        desktop: [
            'vendor.bundle.js',
            'index.bundle.js',
            '0.bundle.js',
            'ReduxSync.dynamic.bundle.js',
            'DesktopSidebarCardDrop.hvalanka.bundle.js',
        ],
    },
};

describe('Dynamic bundle', function() {
    let browser;

    before(async function() {
        browser = await puppeteer.launch({ headless: true });
    });

    after(async function() {
        await browser.close();
    });

    async function collectScripts(relativeUrl) {
        let scripts = [];
        let isRouterComplete = false;

        let url = HOST + '/collections' + relativeUrl;
        let page = await browser.newPage();

        await page.setRequestInterception(true);

        page.on('request', async(req) => {
            await req.continue();

            if (/\.js$/.test(req.url())) {
                scripts.push(normalizeUrl(req.url()));
            }
        });

        page.on('metrics', (metric) => {
            if (metric.title === 'router_end') {
                isRouterComplete = true;
            }
        });

        await page.goto(url);

        return filterScripts(scripts);
    }

    for (let [name, { url, mobile, desktop }] of Object.entries(CASES)) {
        it(`${name}: desktop`, async function() {
            this.timeout(10000);
            expect(await collectScripts(url)).to.eql(desktop);
        });

        it(`${name}: mobile`, async function() {
            this.timeout(10000);
            expect(await collectScripts(url + '?mobile=1')).to.eql(mobile);
        });
    }
});
