// @flow

// flowlint-next-line untyped-import: off
import {screen, fireEvent} from '@testing-library/dom';

import {makeMirror} from '@self/platform/helpers/testament';
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
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';

import {
    SUPPLIER_INFO,
    SUPPLIER_ID,
    FULL_NAME,
    INN,
    PRODUCT,
    OFFER,
    OFFER_ID,
    PRODUCT_ID,
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

const WIDGET_PATH = require.resolve('@self/platform/widgets/content/KMDefaultOffer');

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            ...user,
        },
        request: {
            cookie,
            params: {
                productId: PRODUCT_ID,
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
        createProduct(PRODUCT, PRODUCT_ID),
        createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
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

describe('Widget: KMDefaultOffer', () => {
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
        jestLayer = mirror.getLayer('jest');
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Подписочный оффер', () => {
        beforeAll(async () => {
            mockIntersectionObserver();
            await setReportState();
            await jestLayer.runCode(() => {
                jest.mock(
                    '@self/platform/widgets/pages/ProductPage',
                    () => ({SKU_ID_QUERY_PARAM: 'sku'})
                );
            }, []);
        });

        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        describe.skip('Юридическая информация о СМЗ', () => {
            test('Отображается в инфо-подсказке (HintWithContent)', async () => {
                await makeContext();
                const {container} = await apiaryLayer.mountWidget(WIDGET_PATH);

                const infoIcon = container.querySelector('svg.infoIcon');
                expect(infoIcon).toBeInTheDocument();

                fireEvent.mouseOver(infoIcon);
                const fullNameText = await screen.findByText(FULL_NAME);
                expect(fullNameText).toBeInTheDocument();


                fireEvent.mouseOver(infoIcon);
                const innText = await screen.findByText(`ИНН: ${INN}`);
                expect(innText).toBeInTheDocument();

                const contactAddress = screen.findByText(new RegExp(CONTACT_ADRESS, 'g'));
                expect(contactAddress).not.toBe();
            });
        });
    });
});
