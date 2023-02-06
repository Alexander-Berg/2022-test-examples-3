// mocks
import * as cashback from '@self/root/src/spec/hermione/kadavr-mock/report/cashback';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckoutPage, addPresetForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';
import {
    prepareCashbackProfile,
    prepareCashbackOptions,
    prepareCashbackParams,
} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/cashback';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {CASHBACK_PROFILE_TYPES} from '@self/root/src/entities/cashbackProfile';
import ADDRESS from '@self/root/src/spec/hermione/kadavr-mock/checkouter/addresses';
import {DEFAULT_CONTACT} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/contacts';

const CASHBACK_AMOUNT = 123;

export const prepareCheckoutPageWithCashback = {
    name: 'Готовим страницу с заказом с возможностью списания/начисления кешбэка',
    async func({
        // Доступна ли опция начисления
        allowEmit = false,
        // Доступна ли опция списания
        allowSpend = false,
        // Размер кешбэка
        cashbackAmount = CASHBACK_AMOUNT,
        // Выбранная опция кешбэка, по дефолту начисление
        defaultSelectedOption = CASHBACK_PROFILE_TYPES.EMIT,
    }) {
        // Подготавливаем оффер для оформления
        const cart = buildCheckouterBucket({
            items: [{
                skuMock: cashback.skuMock,
                offerMock: cashback.offerMock,
                count: 1,
            }],
            properties: {
                yandexPlusUser: true,
            },
            deliveryOptions: [deliveryDeliveryMock],
        });

        // подготавливаем опции кешбэка
        const cashbackOptionsProfiles = prepareCashbackProfile({
            cartId: cart.label,
            offerId: cashback.offerMock.feed.offerId,
            cashbackAmount,
            feedId: cashback.offerMock.feed.id,
        });
        const cashbackOptions = prepareCashbackOptions(cashbackAmount);
        const cashbackParams = prepareCashbackParams({
            cashbackAmount,
            allowEmit,
            allowSpend,
        });

        const cartState = await this.browser.yaScenario(
            this,
            prepareMultiCartState,
            [cart],
            {
                additionalCheckouterCollections: {
                    cashbackOptionsProfiles,
                    cashbackOptions,
                    cashbackBalance: cashbackAmount,
                    cashback: cashbackParams,
                    selectedCashbackOption: defaultSelectedOption,
                },
            }
        );

        // Создаем заказы пользователяя, что бы была предзаполненная форма
        const orders = {};
        /** Минимальное наполнение ордерами для повторного заказа **/
        for (let id = 0; id < 5; id++) {
            orders[id] = {
                id,
                delivery: {deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET},
            };
        }
        await this.browser.setState('Checkouter.collections.order', orders);
        await this.browser.setState('persAddress.lastState', {
            paymentType: PAYMENT_TYPE.PREPAID,
            paymentMethod: PAYMENT_METHOD.YANDEX,
            contactId: null,
            parcelsInfo: null,
        });
        // Выставялем предвыбранные параметры для оформления заказа
        await this.browser.yaScenario(
            this,
            addPresetForRepeatOrder,
            {
                address: ADDRESS.MOSCOW_ADDRESS,
                contact: DEFAULT_CONTACT,
            }
        );

        // Открываем страницу чекаута с нужным оффером
        await this.browser.yaScenario(
            this,
            prepareCheckoutPage,
            {
                items: cartState.checkoutItems,
                reportSkus: cartState.reportSkus,
            }
        );
    },
};

export const prepareActualizedCheckoutResponse = {
    name: 'Актуализированный ответ чекатуреа',
    async func({
        isEmitAllow = true,
        isSpendAllow = false,
        cashbackAmount = CASHBACK_AMOUNT,
    }) {
        const cashbackOptions = prepareCashbackOptions(cashbackAmount);
        const cashbackParams = prepareCashbackParams({
            cashbackAmount,
            allowEmit: isEmitAllow,
            allowSpend: isSpendAllow,
        });

        const actualizeCheckouterCollections = {
            cashbackOptions,
            cashbackBalance: cashbackAmount,
            cashback: cashbackParams,
        };
        await this.browser.setState('Checkouter.collections', actualizeCheckouterCollections);
    },
};
