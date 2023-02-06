import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeSuite, makeCase} from 'ginny';

import ChoosingService from '@self/root/src/components/ChoosingService/__pageObject/index.desktop';
import {NON_SELECTED_TEXT} from '@self/root/src/components/OfferServiceCard';
import OfferServiceCard from '@self/root/src/components/OfferServiceCard/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import productWithCPADO from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CartPopup
    from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';

const SERVICE_ID_MOCK1 = 1;
const SERVICE_TITLE_MOCK1 = 'Установка1';
const SERVICE_ID_MOCK2 = 2;
const SERVICE_TITLE_MOCK2 = 'Установка2';

export default makeSuite('КМ', {
    id: 'MARKETFRONT-57688',
    environment: 'kadavr',
    feature: 'Доп услуги на КМ',
    issue: 'MARKETFRONT-57701',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                cartPopup: () => this.createPageObject(CartPopup),
                choosingService: () => this.createPageObject(ChoosingService),
                offerServiceCard: () => this.createPageObject(OfferServiceCard),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
            });

            const {offerMock, product, route} = productWithCPADO;
            const offer = createOffer({
                ...offerMock,
                services: [
                    {
                        service_id: SERVICE_ID_MOCK1,
                        title: SERVICE_TITLE_MOCK1,
                        price: {
                            value: '42',
                            currency: 'RUR',
                        },
                    },
                    {
                        service_id: SERVICE_ID_MOCK2,
                        title: SERVICE_TITLE_MOCK2,
                        price: {
                            value: '43',
                            currency: 'RUR',
                        },
                    },
                ],
            }, offerMock.wareId);

            const state = mergeState([
                product,
                offer,
            ]);

            await this.browser.setState('report', state);

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, route);
        },

        'Сохранение выбранной услуги в ДО добавленного в корзину товара': makeCase({
            async test() {
                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга изначально не выбрана');

                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(SERVICE_ID_MOCK1);
                await this.popupChoosingService.saveButtonClick();
                await this.cartButton.click();
                await this.cartPopup.waitForAppearance();
                await this.browser.refresh();

                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    SERVICE_TITLE_MOCK1,
                    'Выбранная услуга сохраняется после обновления страницы'
                );
            },
        }),

        'Изменение выбранной услуги в ДО добавленного в корзину товара': makeCase({
            async test() {
                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(SERVICE_ID_MOCK1);
                await this.popupChoosingService.saveButtonClick();
                await this.cartButton.click();
                await this.cartPopup.waitForAppearance();
                await this.browser.refresh();

                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal(SERVICE_TITLE_MOCK1, 'Сначала выбрана 1-я услуга');

                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(SERVICE_ID_MOCK2);
                await this.popupChoosingService.saveButtonClick();
                await this.browser.refresh();

                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    SERVICE_TITLE_MOCK2,
                    'Выбор 2-й услуги сохраняется после обновления страницы'
                );
            },
        }),

        'Попап апсейла': makeCase({
            async test() {
                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга изначально не выбрана');

                await this.cartButton.click();
                await this.cartPopup.waitForAppearance();
                await this.choosingService.addService(SERVICE_ID_MOCK1);
                await this.cartPopup.close();
                await this.browser.refresh();

                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    SERVICE_TITLE_MOCK1,
                    'Выбранная услуга сохраняется после обновления страницы'
                );
            },
        }),
    },
});
