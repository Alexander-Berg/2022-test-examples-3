const {
    getPageUrl,
    getTestModeParams,
    metrics,
    hooks,
} = require(`${__dirname}/../../../configs/porchmark.base.conf.js`);

const sampleHost = process.env.SAMPLE_HOST || 'https://default.exp-touch.tst.market.yandex.ru';

const testHost = process.env.TEST_HOST;
const page = process.env.TEST_PAGE;
const testMode = process.env.TEST_MODE || 'normal';

const url = process.env.TEST_PAGE_URL || getPageUrl(page);

const {
    iterations,
    recordWprCount,
    selectWprCount,
    warmIterations,
} = getTestModeParams(testMode);

module.exports = {
    mode: 'puppeteer',
    workers: 2,
    iterations,
    pageTimeout: 60,
    puppeteerOptions: {
        headless: true,
        useWpr: true,
        recordWprCount,
        selectWprCount,
        selectWprMethod: 'closestByHtmlSize',
        cacheEnabled: false,
        warmIterations,
        retryCount: 20,
    },
    browserProfile: {
        mobile: true,
    },
    comparisons: [
        {
            name: page,
            sites: [
                {
                    name: 'sample',
                    url: `${sampleHost}${url}`,
                },
                {
                    name: 'test',
                    url: `${testHost}${url}`,
                },
            ],
        },
    ],
    stages: {
        recordWpr: true,
        compareMetrics: true,
    },
    metrics,
    hooks,
};
