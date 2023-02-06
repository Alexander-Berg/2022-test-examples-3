import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeSuite, makeCase} from 'ginny';

import {NON_SELECTED_TEXT} from '@self/root/src/components/OfferServiceCard';
import OfferServiceCard from '@self/root/src/components/OfferServiceCard/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import {
    phoneProductRoute,
    productWithCPADefaultOfferAndServices,
} from '@self/platform/spec/hermione/fixtures/product';
import CartPopup from '@self/platform/spec/page-objects/widgets/content/CartPopup';

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
                offerServiceCard: () => this.createPageObject(OfferServiceCard),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
            });

            const dataMixin = {
                data: {
                    search: {
                        total: 1,
                        totalOffers: 1,
                    },
                },
            };
            await this.browser.setState('Carter.items', []);
            await this.browser.setState('report', mergeState([
                productWithCPADefaultOfferAndServices,
                dataMixin,
            ]));

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, {lr: 213, ...phoneProductRoute});
        },

        'Сохранение выбранной услуги в ДО добавленного в корзину товара': makeCase({
            async test() {
                await this.browser.yaWaitForPageReady();
                await this.offerServiceCard.waitForRootVisible(1000);
                const offerServiceCardSelector = await this.offerServiceCard.getSelector();
                await this.browser.scroll(offerServiceCardSelector);
                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга изначально не выбрана');

                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                const cartButtonSelector = await this.cartButton.getSelector();
                await this.browser.scroll(cartButtonSelector);
                await this.cartButton.click();
                await this.browser.waitForVisible(CartPopup.root, 10000);
                await this.cartPopup.waitForText('Товар в корзине');
                await this.browser.refresh();

                await this.browser.yaWaitForPageReady();
                await this.offerServiceCard.waitForRootVisible(1000);
                await this.browser.scroll(offerServiceCardSelector);
                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    'Установка1',
                    'Выбранная услуга сохраняется после обновления страницы'
                );
            },
        }),

        'Изменение выбранной услуги добавленного в корзину товара': makeCase({
            async test() {
                await this.browser.yaWaitForPageReady();
                await this.offerServiceCard.waitForRootVisible(1000);
                const offerServiceCardSelector = await this.offerServiceCard.getSelector();
                await this.browser.scroll(offerServiceCardSelector);
                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                const cartButtonSelector = await this.cartButton.getSelector();
                await this.browser.scroll(cartButtonSelector);
                await this.cartButton.click();
                await this.browser.waitForVisible(CartPopup.root, 10000);
                await this.cartPopup.waitForText('Товар в корзине');
                await this.browser.refresh();

                await this.browser.yaWaitForPageReady();
                await this.offerServiceCard.waitForRootVisible(1000);
                await this.browser.scroll(offerServiceCardSelector);
                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal('Установка1', 'Сначала выбрана 1-я услуга');

                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(2);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceCard.waitForRootVisible(1000);
                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    'Установка2',
                    'Выбрана 2-я услуга'
                );

                await this.browser.refresh();
                await this.browser.yaWaitForPageReady();
                await this.offerServiceCard.waitForRootVisible(1000);
                await this.browser.scroll(offerServiceCardSelector);
                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    'Установка2',
                    'Выбор 2-й услуги сохраняется после обновления страницы'
                );
            },
        }),
    },
});
