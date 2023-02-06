import {makeMirror} from '@self/platform/helpers/testament';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import PricePO from '@self/platform/components/Price/__pageObject';
import productFarma from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/productFarma';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const widgetPath = require.resolve('@self/platform/widgets/content/KMDefaultOffer');

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';
    const cookie = {
        kadavr_session_id: await kadavrLayer.getSessionId(),
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            params: {
                productId: productFarma.productId,
            },
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

// SKIPPED MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: KMDefaultOffer.', () => {
    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    // testpalm: marketfront-5217
    test('медицинского товара содержит префикс "от"', async () => {
        await kadavrLayer.setState('report', productFarma.state);
        await makeContext({cookies: {purchaseList: 1}});

        const {container} = await apiaryLayer.mountWidget(widgetPath);
        const price = container.querySelector(PricePO.price);

        expect(price).toBeTruthy();
        expect(price.textContent).toContain('от');
    });
});
