import {makeMirrorTouch} from '@self/root/src/helpers/testament/mirror';
import {
    checkoutCartId as dsbsCheckoutCartId,
} from '@self/root/src/widgets/content/checkout/common/__spec__/mockData/dsbs';

import {
    baseMockFunctionality,
} from './mockFunctionality';
import {
    hasGroupExpectedTitle,
} from './testCases';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext({cookies = {}, exps = {}, user = {}}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {
        ...cookies,
    };

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

beforeAll(async () => {
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

    await makeContext({});
});

afterAll(() => {
    mirror.destroy();
});

const widgetPath = '@self/root/src/widgets/content/checkout/common/CheckoutParcels';

describe('CheckoutParcels', () => {
    describe('Должен содержать ожидаемые данные для DSBS офера', () => {
        let widgetContainer;
        beforeAll(async () => {
            await jestLayer.backend.runCode(baseMockFunctionality, ['@self/root/src/widgets/content/checkout/common/__spec__/mockData/dsbs']);
            const {container} = await apiaryLayer.mountWidget(widgetPath, {
                visibleCheckoutCartIds: [dsbsCheckoutCartId],
            });
            widgetContainer = container;
            await makeContext({});
        });
        test('содержит правильный заголовок', async () => {
            await hasGroupExpectedTitle(widgetContainer, {
                tag: 'H2',
                value: 'Посылки',
            });
        });
    });
});
