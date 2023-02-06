import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';
import assert from 'assert';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {replaceBreakChars} from '@self/root/src/spec/utils/text';

import {
    createReturnPositiveScenario,
    checkReturnReasonOptionsValidation,
} from '@self/root/src/spec/hermione/scenarios/returns';

import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject';
import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import {ReturnItemReason} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItemReason/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {Submit} from '@self/root/src/widgets/parts/ReturnCandidate/components/Submit/__pageObject';
import {Final} from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/__pageObject';
import SpecificFinalText from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/components/SpecificFinalText/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import ReturnMapOutletInfo from '@self/root/src/widgets/parts/ReturnCandidate/widgets/ReturnMapOutletInfo/__pageObject';

import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES, DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import RETURN_TEXT_CONSTANTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';
import returnOptionsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import returnOutletsMock, {pickPointPostamat}
    from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';


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
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            wareMd5: checkoutItemIds.asus.offerId,
            count: 1,
            id: ID,
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
                    returnsForm: () => this.createPageObject(ReturnsPage),
                    returnItemsScreen: () => this.createPageObject(
                        ReturnItems,
                        {parent: this.returnsForm}
                    ),
                    buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsForm}),
                    reasonTypeSelector: () =>
                        this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                    returnsMoney: () => this.createPageObject(Account, {parent: this.returnsForm}),
                    bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
                    recipientForm: () => this.createPageObject(RecipientForm, {parent: this.returnsForm}),
                    submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                    finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
                    returnsFinalFullfilmentText: () => this.createPageObject(SpecificFinalText, {parent: this.finalScreen}),
                    returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                    returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                });

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

                const SHOP_ID = 101;
                const shopInfo = createShopInfo({
                    returnDeliveryAddress: 'hello, there!',
                    shopName: 'MockedShopName',
                }, SHOP_ID);

                await this.browser.setState('ShopInfo', {
                    returnContacts: [],
                });

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
                        shopId: SHOP_ID,
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

                await this.order.returnsButton.isVisible().should.eventually.to.be
                    .equal(true, 'Кнопка Вернуть заказ должна быть видна');

                await switchTab.call(this);

                await this.submitForm.isButtonDisabled().should.eventually.to.be
                    .equal(true, 'Кнопка Продолжить должна быть заблокирована');
            },
        },

        makeSuite('Выбор опции возврата.', {
            id: 'marketfront-5699',
            issue: 'MARKETPROJECT-8485',
            story: {
                async beforeEach() {
                    assert(
                        this.reasonTypeSelector, 'PageObject.ReasonTypeSelector must be defined in returns.fillForm scenario'
                    );
                    await this.reasonTypeSelector.isReasonInputVisible().should.eventually.to.be
                        .equal(false, 'Поле ввода причины возврата должно быть скрыто');

                    await this.reasonTypeSelector.isPhotoUploadHeaderVisible().should.eventually.to.be
                        .equal(false, 'Загрузчик изображений должен быть скрыт');
                },
                'Опция "Есть недостатки"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonBadQuality();
                    },
                }),
                'Опция "Не подошел"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonDoNotFit();
                    },
                }),
                'Опция "Привезли не то"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonWrongItem();
                    },
                }),
                async afterEach() {
                    await this.reasonTypeSelector.waitForReasonInputVisible();

                    await this.reasonTypeSelector.isReasonInputVisible().should.eventually.to.be
                        .equal(true, 'Поле ввода причины возврата должно быть видно');

                    await this.reasonTypeSelector.isPhotoUploadHeaderVisible().should.eventually.to.be
                        .equal(true, 'Загрузчик изображений должен быть виден');

                    await this.submitForm.isButtonDisabled().should.eventually.to.be
                        .equal(false, 'Кнопка Продолжить должна быть разблокирована');
                },
            },
        }),

        makeSuite('Валидация выбора опций возврата.', {
            id: 'marketfront-5700',
            issue: 'MARKETPROJECT-8485',
            story: {
                'Опция "Есть недостатки"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonBadQuality();
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {});
                    },
                }),
                'Опция "Не подошел"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonDoNotFit();
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {
                            withPhotoUpload: false,
                        });
                    },
                }),
                'Опция "Привезли не то"': makeCase({
                    async test() {
                        await this.reasonTypeSelector.setReasonWrongItem();
                        return this.browser.yaScenario(this, checkReturnReasonOptionsValidation, {});
                    },
                }),
            },
        }),

        makeSuite('Предоплатный заказ.', {
            id: 'bluemarket-2558',
            issue: 'BLUEMARKET-4949',
            /**
             * Должен быть testing
             * Нельзя создать возврат на DELIVERED заказ, переведенный в DELIVERED только что, поэтому kadavr
             */
            environment: 'kadavr',
            story: {
                'Заявление на возврат оформляется': makeCase({
                    test() {
                        return this.browser.yaScenario(this, createReturnPositiveScenario, {
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
            id: 'bluemarket-2557',
            issue: 'BLUEMARKET-4949',
            /**
             * Должен быть testing
             * Нельзя создать PREPAID DELIVERED заказ через ручками, поэтому kadavr
             */
            environment: 'kadavr',
            story: {
                'Заявление на возврат оформляется': makeCase({
                    test() {
                        return this.browser.yaScenario(this, createReturnPositiveScenario, {
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
                            shouldMapStepBeShown: false,
                        });
                    },
                }),
            },
        }),

        makeSuite('Экран успешного оформления заявления', {
            id: 'bluemarket-2607',
            issue: 'BLUEMARKET-5710',
            environment: 'kadavr',
            story: {
                beforeEach() {
                    return this.browser.yaScenario(this, createReturnPositiveScenario, {
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

async function switchTab() {
    const tabIds = await this.browser.getTabIds();
    await this.order.clickReturnButton();
    const newTabId = await this.browser.yaWaitForNewTab({startTabIds: tabIds});
    await this.allure.runStep(
        'Переключаемся на новую вкладку, проверяем, что мы на форме оформления возвратов',
        async () => {
            await this.browser.switchTab(newTabId);
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
        }
    );
}
