import {makeSuite, makeCase, mergeSuites} from 'ginny';

import {EXPRESS_ADDRESS_ID_COOKIE, EXPRESS_LOCATION_COOKIE} from '@self/root/src/constants/expressService';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {
    setExpressAddress,
    setKurganAddressToList,
    unsetExpressAddress,
} from '@self/root/src/spec/hermione/scenarios/express';

// page-objects
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import LastDeliveryPopup from '@self/root/src/widgets/content/ExpressAddressPopup/components/LastDeliveryAddress/__pageObject/';
import ChoiceAddressPopup from '@self/root/src/widgets/content/ExpressAddressPopup/components/ChoiceAddress/__pageObject/';
import DeliveryUnavailable from '@self/root/src/widgets/content/ExpressAddressPopup/components/DeliveryUnavailable/__pageObject';
import ExpressAddressPopup from '@self/root/src/widgets/content/ExpressAddressPopup/components/View/__pageObject';
import CartEntryPoint from '@self/root/src/components/CartEntryPoint/__pageObject/index.desktop';

/**
 * Тест на работу кнопки "В корзину"
 */
export default makeSuite('Кнопка "В корзину"', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                lastDeliveryPopup: () => this.createPageObject(LastDeliveryPopup),
                expressPopup: () => this.createPageObject(ExpressAddressPopup),
                choiceAddressPopup: () => this.createPageObject(ChoiceAddressPopup),
                cartEntryPoint: () => this.createPageObject(CartEntryPoint),
                deliveryUnavailable: () => this.createPageObject(DeliveryUnavailable),
            });
        },
        'По клику,': {
            'если есть последний адрес доставки,': mergeSuites({
                async beforeEach() {
                    await this.browser.yaScenario(this, unsetExpressAddress, {withReload: false});
                    await this.browser.yaScenario(this, setKurganAddressToList);
                    await this.browser.setState('Checkouter.collections.order', {
                        1: {
                            id: 1,
                            shopOrderId: 1,
                            delivery: deliveryDeliveryMock,
                        },
                    });

                    await this.browser.yaWaitForPageReady();
                    await this.cartButton.click();

                    await this.lastDeliveryPopup.isVisible();
                    await this.cartEntryPoint.isVisible();
                },
                'появляется попап с адресом последней доставки': makeCase({
                    id: 'marketfront-4989',
                    issue: 'MARKETFRONT-51889',
                    async test() {
                        const address = await this.lastDeliveryPopup.getAddress();
                        await this.expect(address).to.be.equal('Test, street, д. house', 'Адрес подставился');
                    },
                }),
                'При клике на "Другой" появляется попап выбора адреса': makeCase({
                    id: 'marketfront-4991',
                    issue: 'MARKETFRONT-51889',
                    async test() {
                        await this.lastDeliveryPopup.changeAddress();
                        await this.expressPopup.waitForAddressesListVisible();
                    },
                }),
                'При клике на "Верно" выбирается этот адрес и оффер не доступен по этому адресу': makeCase({
                    id: 'marketfront-4990',
                    issue: 'MARKETFRONT-51889',
                    async test() {
                        await this.browser.setState('report', {
                            data: {
                                search: {
                                    total: 0,
                                    results: [],
                                },
                                filters: [],
                                sorts: [],
                                intents: [],
                                offers: [],
                            },
                            collections: {},
                        });
                        await this.lastDeliveryPopup.selectAddress();
                        await this.deliveryUnavailable.waitForVisible()
                            .should.eventually.be.equal(
                                true,
                                'Попап с информацией о невозможности доставки должен быть виден'
                            );
                    },
                }),
                'При клике на "Верно" выбирается этот адрес и оффер добавляется в корзину': makeCase({
                    id: 'marketfront-4990',
                    issue: 'MARKETFRONT-51889',
                    async test() {
                        await this.lastDeliveryPopup.selectAddress();

                        await this.cartEntryPoint.waitUntilCounterChanged('1', 20000);

                        this.browser.waitUntil(async () => {
                            const addressId = await this.browser.getCookie(this, EXPRESS_ADDRESS_ID_COOKIE)
                                .then(value => (value && value.value) || undefined);

                            const expressAddress = await this.browser.getCookie(this, EXPRESS_LOCATION_COOKIE)
                                .then(value => (value && value.value) || undefined);

                            return addressId !== undefined && expressAddress !== undefined;
                        }, 5000, 'Кука addressId или express-address не изменилась');
                    },
                }),
            }),
            'если нет последнего адреса доставки,': mergeSuites({
                async beforeEach() {
                    await this.browser.yaScenario(this, setKurganAddressToList);
                    await this.browser.yaWaitForPageReady();
                },
                'и нет куки, то появляется попап-предложение ввести адрес': makeCase({
                    id: 'marketfront-4995',
                    issue: 'MARKETFRONT-51894',
                    async test() {
                        await this.cartButton.click();
                        await this.choiceAddressPopup.isVisible();
                        await this.choiceAddressPopup.clickNext();
                        await this.expressPopup.waitForAddressesListVisible();
                    },
                }),
                'и есть кука, то оффер добавляется в корзину без попапа': makeCase({
                    id: 'marketfront-4988',
                    issue: 'MARKETFRONT-51894',
                    async test() {
                        await this.browser.yaScenario(this, setExpressAddress);
                        await this.cartButton.click();
                        await this.cartEntryPoint.waitUntilCounterChanged('1', 10000);
                        await this.browser.waitForVisible(ExpressAddressPopup.root, 5000, true); // попап гиперлокальности НЕ показался
                    },
                }),
            }),
        },
    }),
});
