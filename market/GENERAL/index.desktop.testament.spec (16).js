import {screen} from '@testing-library/dom';
import {makeMirror} from '@self/platform/helpers/testament';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

// mocks
import {
    createProduct,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {
    createDelivery,
    createDeliveryOption,
} from '@self/project/src/entities/delivery/__mock__/delivery.mock';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';

// page objects
import TopOffersPO from '@self/platform/widgets/content/TopOffers/components/View/__pageObject';
import TopOffersMiniPO from '@self/platform/widgets/content/TopOffers/components/ViewMini/__pageObject';
import DeliveryInfo from '@self/platform/spec/page-objects/components/DeliveryInfo';
import OfferPhotoThumbnail from '@self/platform/components/OfferPhotoThumbnail/__pageObject';
import PricePO from '@self/platform/components/Price/__pageObject';

// flowlint-next-line untyped-import:off
import {
    OFFER_ID1,
    OFFER_ID2,
    createTopOffersProduct,
    SLUG,
    PRODUCT_ID,
    createTopOffer,
    defaultOfferData,
} from './mocks';


/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
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

describe('Widget: TopOffers', () => {
    const WIDGET_PATH = require.resolve('@self/platform/widgets/content/TopOffers');
    const widgetOptions = {
        props: {
            mode: 'full',
            productId: PRODUCT_ID,
            isCutPrice: false,
        },
    };

    const setReportState = async offerData => {
        const reportState = mergeState([
            createTopOffersProduct(),
            createTopOffer(offerData, OFFER_ID1),
            createTopOffer(offerData, OFFER_ID2),
            {
                data: {
                    search: {
                        total: 2,
                        totalOffers: 2,
                        totalOffersBeforeFilters: 2,
                    },
                },
            },
        ]);
        await kadavrLayer.setState('report', reportState);
    };

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
        jestLayer = mirror.getLayer('jest');
        mandrelLayer = mirror.getLayer('mandrel');
        apiaryLayer = mirror.getLayer('apiary');
        kadavrLayer = mirror.getLayer('kadavr');

        // $FlowFixMe
        jest.useFakeTimers('modern');
        // $FlowFixMe
        jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));

        await jestLayer.doMock(
            require.resolve('@self/platform/widgets/content/OfferModifications'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/platform/widgets/content/TableSizePopup'),
            () => ({create: () => Promise.resolve(null)})
        );
        await jestLayer.doMock(
            require.resolve('@self/project/src/utils/router'),
            () => ({
                buildUrl: () => '',
                buildURL: () => '',
            })
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    // testpalm: marketfront-5218
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('Отсутствует мультиоффер у лекарств', () => {
        const widgetParams = {
            productId: PRODUCT_ID,
            isCutPrice: false,
        };
        const categories = [{
            entity: 'category',
            id: 91494,
            name: 'Лекарства',
            fullName: 'Лекарства',
            type: 'guru',
            isLeaf: true,
            slug: 'medicine',
            kinds: ['medicine'],
        }];
        const specs = {
            internal: [{value: 'medicine'}],
        };

        const setReportState = async offerData => {
            const offer = createOffer({
                ...defaultOfferData,
                categories,
                specs,
                isCutPrice: false,
                benefit: {
                    type: 'default',
                },
                ...offerData,
            });

            const reportState = mergeState([
                createProduct({
                    slug: SLUG,
                    type: 'model',
                }, PRODUCT_ID),
                createOfferForProduct(offer, PRODUCT_ID, OFFER_ID1),
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

        beforeEach(async () => {
            await makeContext({purchaseList: '1'});
        });


        test('в мини формате (compact)', async () => {
            await setReportState();
            const {container} = await apiaryLayer.mountWidget(
                WIDGET_PATH,
                {
                    props: {
                        mode: 'compact',
                        ...widgetParams,
                    },
                }
            );

            expect(container.querySelector(TopOffersMiniPO.root)).toBeNull();
        });

        test('при обычном размере', async () => {
            await setReportState();
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, {props: widgetParams});

            expect(container.querySelector(TopOffersPO.root)).toBeNull();
        });
    });

    describe('Информация о доставке у офера', () => {
        const compareText = async (container, expectedText) => {
            await step('Ищем компонент DeliveryInfo и сравниваем текст', async () => {
                const text = container.querySelector(DeliveryInfo.pickupInfo).textContent;
                expect(text).toEqual(expectedText);
            });
        };

        test('с долгим сроком доставки (marketfront-5973)', async () => {
            // $FlowFixMe
            jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
            await makeContext();

            const deliveryOption = createDeliveryOption({
                dayFrom: 10, isDefault: true, isEstimated: true,
            });
            const deliveryMock = createDelivery({options: [deliveryOption]});
            await setReportState({delivery: deliveryMock});

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            await compareText(container, 'Курьером с 8 июня — 30 ₽');
            // // $FlowFixMe
            jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
        });

        test('с признаком товара под заказ (marketfront-5972)', async () => {
            // $FlowFixMe
            jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
            await makeContext();

            const deliveryOption = createDeliveryOption({
                dayFrom: 10, isDefault: true, isEstimated: true,
            });
            const deliveryMock = createDelivery({options: [deliveryOption]});
            await setReportState({
                delivery: deliveryMock,
                isUniqueOffer: true,
                orderReturnPolicy: {
                    type: 'forbidden',
                    reason: 'unique-order',
                    description: 'Some description',
                },
                orderCancelPolicy: {
                    type: 'time-limit',
                    reason: 'unique-order',
                    daysForCancel: 10,
                    description: 'Some description',
                },
            });

            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);

            await compareText(container, 'Курьером с 8 июня — 30 ₽');
            // $FlowFixMe
            jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
        });
    });

    describe('Блок "Топ 6."', () => {
        test('DSBS-оффер в топ-6. Должен содержать основные данные', async () => {
            await makeContext();
            await setReportState(offerDSBSMock);
            const {container} = await apiaryLayer.mountWidget(WIDGET_PATH, widgetOptions);
            const deliveryInfoText = 'Доставка продавца';

            const topOffer = container.querySelector(TopOffersPO.root);
            const price = topOffer.querySelector(PricePO.price);
            const image = topOffer.querySelector(OfferPhotoThumbnail.root);
            const shopName = screen.getAllByText(`${offerDSBSMock.shop.name}`)[0];
            const deliveryInfo = screen.getAllByText(deliveryInfoText, {exact: false})[0];
            const gradesCount = screen.getAllByRole('link', {name: '3 219 отзывов'})[0];

            expect(image).toBeVisible();
            expect(price.textContent).toContain('2 680 ₽');
            expect(gradesCount.textContent).toContain('3 219 отзывов');
            expect(deliveryInfo.textContent).toContain('Доставка продавца');
            expect(shopName.textContent).toContain(offerDSBSMock.shop.name);
        });
    });
});
