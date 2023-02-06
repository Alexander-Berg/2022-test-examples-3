import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

import {baseMockFunctionality} from './mockFunctionality';
import {hasPlusSubscriptionLegal} from './testCases';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorTouch({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');

    await baseMockFunctionality(jestLayer);
    await jestLayer.runCode(() => {
        jest.mock(
            '@self/root/src/widgets/content/checkout/common/CheckoutAgreementNote/View',
            () => jest.requireActual('@self/root/src/widgets/content/checkout/common/CheckoutAgreementNote/View/index.touch.js')
        );
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

// Расскип https://st.yandex-team.ru/MARKETFRONT-80859
// eslint-disable-next-line market/ginny/no-skip
describe('CheckoutAgreementNote', () => {
    /**
     * @expFlag all_station-subscription
     * @ticket MARKETFRONT-57855
     * @start
     */
    describe('Оплата товара по подписке', () => {
        // bluemarket-4166
        test('Подписочный лигал отображатеся', () => hasPlusSubscriptionLegal(jestLayer, apiaryLayer, mandrelLayer));
    });
    /**
     * @expFlag all_station-subscription
     * @ticket MARKETFRONT-57855
     * @end
     */
});
