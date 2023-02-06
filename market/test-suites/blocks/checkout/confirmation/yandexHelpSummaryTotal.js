import {makeSuite, makeCase} from 'ginny';

// scenarios
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage, addPresetForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';


// pageObjects
import YandexHelpSummaryTotal from '@self/root/src/components/YandexHelpSummaryTotal/__pageObject';

// mocks
import {DSBS_CARTS} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/dsbsCarts';
import ADDRESS from '@self/root/src/spec/hermione/kadavr-mock/checkouter/addresses';
import {DEFAULT_CONTACT} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/contacts';
import {DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';

const MAIN_TEXT = 'И ещё в «Помощь рядом»';

export default makeSuite('Блок оплаты', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                yandexHelpSummaryTotal: () => this.createPageObject(YandexHelpSummaryTotal),
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                DSBS_CARTS
            );

            const orders = {};
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

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESS.MOSCOW_ADDRESS,
                    contact: DEFAULT_CONTACT,
                }
            );

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    checkout2: true,
                }
            );
        },
        'Плашка Помощь Рядом': makeCase({
            id: 'marketfront-4590',
            issue: 'MARKETFRONT-60063',
            async test() {
                await this.yandexHelpSummaryTotal.isVisible()
                    .should.eventually.to.be.equal(true, 'Плашка помощи должна отображаться');

                await this.yandexHelpSummaryTotal.isIconVisible()
                    .should.eventually.to.be.equal(true, 'Иконка должна отображаться');

                await this.yandexHelpSummaryTotal.getText()
                    .should.eventually.to.be.equal(MAIN_TEXT, `Основной текст блока содержит "${MAIN_TEXT}"`);

                await this.yandexHelpSummaryTotal.getDonation()
                    .should.eventually.to.be.match(/^\d+ ₽$/, 'Сумма пожертвования корректна');
            },
        }),
    },
});
