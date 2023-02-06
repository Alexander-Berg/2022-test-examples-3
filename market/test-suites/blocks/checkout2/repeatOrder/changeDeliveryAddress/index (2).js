import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {getStringifiedCoordinates} from '@self/root/src/entities/gpsCoordinate/helpers';

import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';

import {ADDRESSES, CONTACTS} from '../constants';

export default makeSuite('Изменение адреса доставки', {
    id: 'marketfront-5903',
    issue: 'MARKETFRONT-79485',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                editPopup: () => this.createPageObject(EditPopup),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
            });

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: [ADDRESSES.MOSCOW_ADDRESS, ADDRESSES.MOSCOW_ALTERNATIVE_ADDRESS],
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );

            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'В блоке заказа.': {
            async beforeEach() {
                await this.confirmationPage.waitForVisible();
            },
            'При изменении адреса доставки.': {
                async beforeEach() {
                    await this.deliveryInfo.click();
                    await this.editPopup.waitForVisibleRoot();
                },

                'В запросе к Чекаутеру отправляются gps координаты адреса доставки.': makeCase({
                    async test() {
                        await this.allure.runStep(
                            'В пресете выбираем другой адрес доставки', async () => {
                                const address = ADDRESSES.MOSCOW_ALTERNATIVE_ADDRESS.address;
                                await this.addressList.clickAddressListItemByAddress(address);
                                await this.editPopup.waitForChooseButtonSpinnerHidden();
                            }
                        );

                        await this.allure.runStep(
                            'Возврат к странице чекаута', async () => {
                                await this.confirmationPage.waitForVisible();
                                await this.preloader.waitForHidden(5000);
                            }
                        );

                        const gpsExpected = getStringifiedCoordinates(ADDRESSES.MOSCOW_ALTERNATIVE_ADDRESS.location);
                        const {request: checkouterCartLastRequest} = await this.browser.yaGetLastKadavrLogByBackendMethod('Checkouter', 'cart');
                        const gpsFromRequest = checkouterCartLastRequest.body.carts[0].delivery?.buyerAddress?.gps;
                        await this.expect(gpsFromRequest).be.equal(
                            gpsExpected,
                            'В запросе на актуализацию указаны gps координаты адреса доставки и координаты соответствуют ожидаемым'
                        );
                    },
                }),
            },
        },
    },
});
