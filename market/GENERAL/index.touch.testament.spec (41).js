import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';

// OFFER TOOLTIP
import {
    baseMockFunctionality as offerBaseMockFunctionality,
    mockWidgetData as offerMockWidgetFunction,
} from './mockFunctionality';
import {TEST_CASES as OFFER_PROMOT_TOOLTIP_CASES} from './testCases/';

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
});

afterAll(() => {
    mirror.destroy();
});

const createTestSuites = (
    testCase,
    widgetPath,
    mockFunction
) => {
    if (testCase.describe) {
        // eslint-disable-next-line jest/valid-describe
        describe(testCase.describe.name, () => {
            testCase.describe.suites.map(suite => createTestSuites(
                suite,
                widgetPath,
                mockFunction
            ));
        });
    } else {
        it(testCase.caseName, async () => {
            await testCase.method(
                widgetPath,
                jestLayer,
                apiaryLayer,
                mandrelLayer,
                mockFunction,
                testCase.mockData,
                testCase.ctxMock,
                testCase.expectedTitle,
                testCase.expectedDescription
            );
        });
    }
};


const WIDGET_PATH = '@self/root/src/widgets/content/PromoTooltip/tooltips/YandexPlusPromoOfferTooltip';

describe('PromoTooltip', () => {
    describe('Карточка офера', () => {
        beforeAll(async () => {
            await offerBaseMockFunctionality(jestLayer);
        });

        OFFER_PROMOT_TOOLTIP_CASES.map(testCase => createTestSuites(
            testCase,
            WIDGET_PATH,
            offerMockWidgetFunction
        ));
    });
});
