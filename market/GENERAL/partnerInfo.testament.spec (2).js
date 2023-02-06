// @flow

// flowlint-next-line untyped-import: off
import {screen, fireEvent} from '@testing-library/dom';

import {
    // flowlint-next-line untyped-import: off
    createProduct,
    // flowlint-next-line untyped-import: off
    createShopInfo,
    // flowlint-next-line untyped-import: off
    mergeState,
    // flowlint-next-line untyped-import: off
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// flowlint-next-line untyped-import: off
import {createSupplierInfo} from '@yandex-market/kadavr/mocks/ShopInfo/helpers';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {makeMirror} from '@self/platform/helpers/testament';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';

import {
    SUPPLIER_INFO,
    SUPPLIER_ID,
    FULL_NAME,
    INN,
    PRODUCT,
    OFFER,
    OFFER_ID,
    CONTACT_ADRESS,
} from './__mock__/partnerInfoMock.js';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    user?: { [string]: mixed },
};

const WIDGET_PATH = require.resolve('./mockWidget');

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        page: {
            pageId: 'touch:product',
        },
        request: {
            cookie,
            params: {
                productId: OFFER_ID,
                sponsored: '1',
            },
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

const setReportState = async () => {
    // $FlowFixMe не видит вложенные сущности
    const offer = createOffer(OFFER);

    const reportState = mergeState([
        createShopInfo(),
        createProduct(PRODUCT, OFFER_ID),
        createOfferForProduct(offer, OFFER_ID, OFFER_ID),
        {
            data: {
                search: {
                    total: 1,
                },
            },
        },
    ]);

    const supplierState = {
        collections: createSupplierInfo(SUPPLIER_INFO, SUPPLIER_ID),
    };

    await kadavrLayer.setState('report', reportState);
    await kadavrLayer.setState('ShopInfo', supplierState);
};
// MARKETFRONT-96354
// eslint-disable-next-line jest/no-disabled-tests
describe.skip('Widget: ProductOffers', () => {
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
        jestLayer = mirror.getLayer('jest');

        window.performance.timing = {
            navigationStart: () => 0,
        };

        await jestLayer.runCode(debugInfo => {
            jest.doMock(
                debugInfo,
                () => ({DevToolsDebugInfoFrame: {create: () => Promise.resolve(null)}})
            );
        }, [
            require.resolve('@yandex-market/mandrel/devTools/DebugInfo'),
        ]);
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('ProductOffers', () => {
        beforeAll(async () => {
            mockIntersectionObserver();
            await setReportState();
        });

        describe('Юридическая информация о СМЗ', () => {
            test('Отображается ИНН и ФИО в попап после клика', async () => {
                await makeContext();

                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const infoButton = container.querySelector('#scroll-to-productOffersCpa > div > div > div:nth-child(5) > div > div:nth-child(3) > div > button');
                expect(infoButton).toBeInTheDocument();

                fireEvent.click(infoButton);

                const fullNameText = await screen.findByText(`Владелец: Самозанятый ${FULL_NAME}`);
                expect(fullNameText).toBeInTheDocument();

                const innText = await screen.findByText(`ИНН: ${INN}`);
                expect(innText).toBeInTheDocument();

                const contactAddress = screen.findByText(new RegExp(CONTACT_ADRESS, 'g'));
                expect(contactAddress).not.toBe();
            });
        });
    });
});
