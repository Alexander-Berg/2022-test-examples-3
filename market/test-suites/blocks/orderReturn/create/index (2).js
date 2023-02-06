import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {replaceBreakChars} from '@self/root/src/spec/utils/text';
import returnOptionsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import returnOutletsMock, {pickPointPostamat}
    from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

import {createReturnPositiveScenario} from '@self/root/src/spec/hermione/scenarios/returns';

import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {Final} from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';
import SpecificFinalText from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/SpecificFinalText/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import ReturnMapOutletInfo from '@self/root/src/widgets/parts/ReturnCandidate/widgets/ReturnMapOutletInfo/__pageObject';

import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES, DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';

const ID = 11111;

export default makeSuite('Оформление заявления на возврат.', {
    params: {
        items: 'Товары',
        paymentType: 'Тип оплаты',
        paymentMethod: 'Метод оплаты',
        isDropship: 'Дропшип заказ',
    },
    defaultParams: {
        items: [{
            id: ID,
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            count: 5,
        }],
        paymentType: PAYMENT_TYPE.PREPAID,
        paymentMethod: PAYMENT_METHOD.YANDEX,
        isDropship: false,
    },
    feature: 'Оформление заявления на возврат.',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    order: () => this.createPageObject(OrderCard),
                    returnsPage: () => this.createPageObject(ReturnsPage),
                    returnItemsScreen: () => this.createPageObject(
                        ReturnItems,
                        {parent: this.returnsPage}
                    ),
                    reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsPage}),
                    buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsPage}),
                    recipientForm: () => this.createPageObject(RecipientForm, {parent: this.buyerInfoScreen}),
                    bankAccountScreen: () => this.createPageObject(Account, {parent: this.returnsPage}),
                    bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsPage}),
                    finalScreen: () => this.createPageObject(Final, {parent: this.returnsPage}),
                    returnsFinalFullfilmentText: () => this.createPageObject(SpecificFinalText, {parent: this.finalScreen}),
                    returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                    returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                });

                const shopId = 101;
                const shopInfo = createShopInfo({
                    returnDeliveryAddress: 'hello, there!',
                }, shopId);

                const {isDropship} = this.params;

                await this.browser.yaScenario(this, setReportState, {
                    state: mergeState(
                        [shopInfo],
                        {
                            data: {
                                results: isDropship ? [] : returnOutletsMock,
                                search: {results: []},
                            },
                        }
                    ),
                });

                await this.browser.setState(
                    'Checkouter.returnOptions',
                    isDropship
                        ? {deliveryOptions: []}
                        : returnOptionsMock
                );

                await this.browser.setState(
                    'Checkouter.returnableItems',
                    this.params.items.map(item => ({
                        ...item,
                        itemId: item.id,
                    }))
                );

                await this.browser.setState('schema', {
                    mdsPictures: [{
                        groupId: 3723,
                        imageName: '2a000001654282aec0648192ce44a1708325',
                    }],
                });

                const result = await this.browser.yaScenario(this, prepareOrder, {
                    region: this.params.region,
                    orders: [{
                        items: this.params.items,
                        deliveryType: DELIVERY_TYPES.DELIVERY,
                        ...(isDropship ? {
                            delivery: {
                                deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                            },
                        } : {}),
                        shopId,
                    }],
                    paymentType: this.params.paymentType,
                    paymentMethod: this.params.paymentMethod,
                    status: ORDER_STATUS.DELIVERED,
                    fulfilment: !isDropship,
                }, {
                    bankDetails: returnsFormData.bankAccount,
                });

                const orderId = result.orders[0].id;
                this.params.orderId = orderId;

                await this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.ORDER, {orderId});

                await this.order.returnButton.isVisible().should.eventually.to.be
                    .equal(true, 'Кнопка Вернуть заказ должна быть видна');

                await this.order.clickReturnButton();

                // Не работает. this.browser === undefined внутри.
                // await this.browser.yaWaitForPageLoadedWithWaiters();
                await this.browser.yaWaitForPageLoaded(3000, ['state']);
                await this.browser.getUrl().should.eventually.to.be
                    .link({
                        pathname: '/my/returns/create',
                        query: {
                            orderId: String(this.params.orderId),
                            type: 'refund',
                        },
                    }, {
                        skipHostname: true,
                        skipProtocol: true,
                    });
            },
        },

        makeSuite('Предоплатный заказ.', {
            id: 'bluemarket-796',
            issue: 'BLUEMARKET-4950',
            /**
             * Должен быть testing
             * Нельзя создать возврат на DELIVERED заказ, переведенный в DELIVERED только что, поэтому kadavr
             */
            environment: 'kadavr',
            story: {
                'Заявление на возврат оформляется': makeCase({
                    test() {
                        return this.browser.yaScenario(this, createReturnPositiveScenario, {
                            itemsIndexes: [1],
                            itemsCount: this.params.items.reduce((acc, item) => acc + item.count, 0),
                            outlet: pickPointPostamat,
                        });
                    },
                }),
            },
        }),

        makeSuite('Постоплатный заказ.', {
            defaultParams: {
                paymentType: PAYMENT_TYPE.POSTPAID,
                paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
            },
            id: 'bluemarket-797',
            issue: 'BLUEMARKET-4950',
            /**
             * Должен быть testing
             * Нельзя создать PREPAID DELIVERED заказ через ручками, поэтому kadavr
             */
            environment: 'kadavr',
            story: {
                'Заявление на возврат оформляется': makeCase({
                    test() {
                        return this.browser.yaScenario(this, createReturnPositiveScenario, {
                            itemsIndexes: [1],
                            itemsCount: this.params.items.reduce((acc, item) => acc + item.count, 0),
                            outlet: pickPointPostamat,
                        });
                    },
                }),
            },
        }),

        makeSuite('Дропшип заказ.', {
            defaultParams: {
                isDropship: true,
            },
            id: 'bluemarket-2606',
            issue: 'BLUEMARKET-5710',
            environment: 'kadavr',
            story: {
                'Заявление на возврат оформляется': makeCase({
                    test() {
                        return this.browser.yaScenario(this, createReturnPositiveScenario, {
                            itemsIndexes: [1],
                            itemsCount: this.params.items.reduce((acc, item) => acc + item.count, 0),
                            shouldMapStepBeShown: false,
                        });
                    },
                }),
            },
        }),

        makeSuite('Экран успешного оформления заявления.', {
            id: 'bluemarket-2607',
            issue: 'BLUEMARKET-5710',
            environment: 'kadavr',
            story: {
                beforeEach() {
                    return this.browser.yaScenario(this, createReturnPositiveScenario, {
                        itemsIndexes: [1],
                        itemsCount: this.params.items.reduce((acc, item) => acc + item.count, 0),
                        outlet: pickPointPostamat,
                    });
                },

                'Тексты.': makeCase({
                    async test() {
                        await this.finalScreen.waitForRootIsVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Экран успешного оформления заявления должен отобразиться'
                            );

                        await this.returnsFinalFullfilmentText.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Содержимое экрана успешного оформления заявления должно отобразиться'
                            );

                        await this.returnsFinalFullfilmentText.getStepTitleTextByIndex(1)
                            .should.eventually.to.be.equal(
                                replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_FIRST_STEP_TITLE),
                                `Заголовок первого шага "${RETURN_TEXT_CONSTANTS.FINAL_FIRST_STEP_TITLE}"`
                            );

                        await this.returnsFinalFullfilmentText.getStepTitleTextByIndex(3)
                            .should.eventually.to.be.equal(
                                replaceBreakChars(RETURN_TEXT_CONSTANTS.FINAL_SEND_PHOTO_TITLE),
                                `Заголовок третьего шага "${RETURN_TEXT_CONSTANTS.FINAL_SEND_PHOTO_TITLE}"`
                            );
                    },
                }),
            },
        })
    ),
});
