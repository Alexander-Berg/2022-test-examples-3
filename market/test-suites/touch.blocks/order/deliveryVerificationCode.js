import assert from 'assert';
import {makeCase, makeSuite} from 'ginny';

import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import deliveryConditionMock from '@self/root/src/spec/hermione/kadavr-mock/deliveryCondition/deliveryCondition';
import {brandedOutletMock} from '@self/root/market/src/spec/hermione/test-suites/outlet/helpers';
import DeliveryVerificationCodeButton from '@self/root/src/components/DeliveryVerificationCodeButton/__pageObject';
import DeliveryVerificationCodeDrawer from '@self/root/src/components/DeliveryVerificationCodeDrawer/__pageObject';

const ORDER_ID = 1234567890;
const ORDER_OUTLET_ID = brandedOutletMock.id;
const ORDER_COLLECTION = {
    [ORDER_ID]: {
        id: ORDER_ID,
        status: 'PICKUP',
        substatus: 'PICKUP_SERVICE_RECEIVED',
        deliveryType: 'PICKUP',
        delivery: {
            type: 'PICKUP',
            purpose: 'PICKUP',
            verificationPart: {
                barcodeData: 'Secret',
                verificationCode: '123-456-789',
            },
            outletId: ORDER_OUTLET_ID,
        },
    },
};

export default makeSuite('Попап получения посылки в ПВЗ по штрихкоду', {
    environment: 'kadavr',
    feature: 'Получение посылки в ПВЗ по штрихкоду',
    issue: 'MARKETFRONT-51592',
    defaultParams: {
        isAuthWithPlugin: true,
        isAuth: true,
    },
    story: {
        async beforeEach() {
            assert(this.params.pageId, 'Param pageId must be defined in order to run this suite');

            this.setPageObjects({
                deliveryVerificationCodeButton: () => this.createPageObject(DeliveryVerificationCodeButton),
                deliveryVerificationCodeDrawer: () => this.createPageObject(DeliveryVerificationCodeDrawer),
            });

            await this.browser.setState('Checkouter.collections.order', ORDER_COLLECTION);
            await this.browser.yaScenario(this, setReportState, {
                state: {
                    data: {
                        results: [brandedOutletMock],
                        search: {results: []},
                        blueTariffs: deliveryConditionMock,
                    },
                    collections: {
                        outlet: {[ORDER_OUTLET_ID]: brandedOutletMock},
                    },
                },
            });
            await this.browser.yaOpenPage(this.params.pageId, {orderId: ORDER_ID});
        },
        'Кнопка "Получить по штрихкоду" отображается': makeCase({
            async test() {
                await this.deliveryVerificationCodeButton.isButtonVisible()
                    .should.eventually.to.be.equal(true, 'Кнопка отображается');
            },
        }),
        'При клике на кнопку "Получить по штрихкоду" открывается попап со штрихкодом': makeCase({
            async test() {
                await this.deliveryVerificationCodeButton.clickButton();

                await this.deliveryVerificationCodeDrawer.isRootVisible()
                    .should.eventually.to.be.equal(true, 'Попап отображается');
            },
        }),
        'При открытии попапа отображаются его элементы': makeCase({
            async test() {
                await this.deliveryVerificationCodeButton.clickButton();

                await this.deliveryVerificationCodeDrawer.isOrderIdVisible()
                    .should.eventually.to.be.equal(true, 'Идентификатор отображается');
                await this.deliveryVerificationCodeDrawer.isBarcodeVisible()
                    .should.eventually.to.be.equal(true, 'Штрихкод отображается');
                await this.deliveryVerificationCodeDrawer.isCodeVisible()
                    .should.eventually.to.be.equal(true, 'Код отображается');
                await this.deliveryVerificationCodeDrawer.isCodeVisible()
                    .should.eventually.to.be.equal(true, 'Кнопка "Понятно" отображается');
            },
        }),
        'При клике на кнопку "Понятно" закрывается попап со штрихкодом': makeCase({
            async test() {
                await this.deliveryVerificationCodeButton.clickButton();
                await this.deliveryVerificationCodeDrawer.clickAcceptButton();

                this.deliveryVerificationCodeDrawer.root.isExisting()
                    .should.eventually.to.be.equal(false, 'Попап не отображается');
            },
        }),
    },
});
