import {makeMirror} from '@self/platform/helpers/testament';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import productWithVatPrice from './__mock__/productWithPriceWithoutVat';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

const WIDGET_PATH = require.resolve('@self/platform/widgets/content/KMDefaultOffer');

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
                productId: productWithVatPrice.productId,
            },
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

describe('Widget: KMDefaultOffer.', () => {
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

    // testpalm: m2b-43 (MARKETFRONT-81935)
    // https://proxy.sandbox.yandex-team.ru/3316532518/index.html#suites/3c14e6b8c45c6647eb6ebfc28c805a87/f22f6fac9fa51a21/
    // skip reason: тест плавает
    test.skip('Карточка содержит цену без НДС', async () => {
        await kadavrLayer.setState('report', productWithVatPrice.state);
        await makeContext({cookies: {purchaseList: 1}});

        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);
        const selector = '[data-auto=\"offer-price-without-vat\"]';

        const price = container.querySelector(selector);

        expect(price).toBeDefined();
        expect(price.textContent).toContain('300');
        expect(price.textContent).toContain('без НДС');
    });
});
