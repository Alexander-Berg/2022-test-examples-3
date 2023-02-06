// @flow

// flowlint-next-line untyped-import: off
import {screen} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';
// flowlint-next-line untyped-import: off
import {createSupplierInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import {
    // flowlint-next-line untyped-import: off
    mergeState,
    // flowlint-next-line untyped-import: off
    createBusinessInfo,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    SUPPLIER_INFO,
    SUPPLIER_ID,
    BUSINESS_ID,
    SUPPLIER_TYPE,
    FULL_NAME,
    INN,
    CONTACT_ADRESS,
} from './__mock__/supplierInfoMock.js';

const WIDGET_PATH = require.resolve('@self/platform/widgets/pages/BusinessInfoPage');

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        request: {
            cookie,
            params: {
                businessId: [BUSINESS_ID.toString()],
            },
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

const setSupplierState = async () => {
    const supplierState = {
        collections: createSupplierInfo(SUPPLIER_INFO, SUPPLIER_ID),
    };
    const reportState = mergeState([
        createBusinessInfo({
            business_name: 'Self Employed',
            shop_name: 'Self Employed',
            shop_info: [{
                shop_id: SUPPLIER_ID,
                supplier_type: SUPPLIER_TYPE,
            }],
        }, BUSINESS_ID),
    ]);

    await kadavrLayer.setState('ShopInfo', supplierState);
    await kadavrLayer.setState('report', reportState);
};

describe('Widget: BusinessInfoPage', () => {
    beforeAll(async () => {
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

    describe('Информация о продавце', () => {
        beforeAll(async () => {
            await setSupplierState();
        });

        describe('Юридическая информация о СМЗ', () => {
            test('Отображается только ИНН и ФИО', async () => {
                await makeContext();

                await apiaryLayer.mountWidget(WIDGET_PATH);

                const fullNameText = await screen.findByText(`Самозанятый ${FULL_NAME}`);
                expect(fullNameText).toBeInTheDocument();

                const innText = await screen.findByText(`ИНН: ${INN}`);
                expect(innText).toBeInTheDocument();

                const contactAddress = screen.findByText(new RegExp(CONTACT_ADRESS, 'g'));
                expect(contactAddress).not.toBe();
            });
        });
    });
});
