import {makeMirror} from '@self/platform/helpers/testament';
import {
    createProduct,
    createSku,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import MorePricesLinkPO from '@self/platform/widgets/content/MorePricesLink/__pageObject';
import {
    OFFER,
    OFFER_ID,
    SKU,
    SKU_ID,
    PRODUCT,
    PRODUCT_ID,
} from './mocks';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {KadavrLayer} */
let kadavrLayer;

async function makeContext(cookies = {}, exps = {}, user = {}) {
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
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

describe('Widget: MorePricesLinkPO', () => {
    const WIDGET_PATH = require.resolve('@self/platform/widgets/content/MorePricesLink');
    const widgetParams = {
        productId: PRODUCT_ID,
        skuId: SKU_ID,
    };

    const setReportState = async offerData => {
        const offer = createOffer(offerData);

        const reportState = mergeState([
            createProduct(PRODUCT, PRODUCT_ID),
            createSku(SKU, SKU_ID),
            createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
            {
                data: {
                    search: {
                        total: 1,
                    },
                },
            },
        ]);
        await kadavrLayer.setState('report', reportState);
    };

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
        apiaryLayer = mirror.getLayer('apiary');
        kadavrLayer = mirror.getLayer('kadavr');
    });

    afterAll(() => {
        mirror.destroy();
    });

    // testpalm: marketfront-5218
    test('отсутствует у лекарств', async () => {
        await makeContext({purchaseList: '1'});
        await setReportState(OFFER);
        const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetParams);

        expect(container.querySelector(MorePricesLinkPO.root)).toBeNull();
    });
});
