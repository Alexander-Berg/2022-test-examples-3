import {makeSuite, makeCase} from 'ginny';

import {NON_SELECTED_TEXT} from '@self/root/src/components/OfferServiceCard';
import OfferServiceCard from '@self/root/src/components/OfferServiceCard/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import {
    DEFAULT_OFFER_WARE_ID,
    productWithCPADefaultOfferAndServices,
} from '@self/platform/spec/hermione/fixtures/product';

export default makeSuite('КО', {
    id: 'MARKETFRONT-57688',
    environment: 'kadavr',
    feature: 'Доп услуги на КО',
    issue: 'MARKETFRONT-57701',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartButton: () => this.createPageObject(CartButton),
                offerServiceCard: () => this.createPageObject(OfferServiceCard),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
            });

            await this.browser.setState('report', productWithCPADefaultOfferAndServices);

            return this.browser.yaOpenPage(PAGE_IDS_COMMON.OFFER, {offerId: DEFAULT_OFFER_WARE_ID, lr: 213});
        },

        'Сохранение выбранной услуги добавленного в корзину товара': makeCase({
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
                await this.cartButton.click();

                await this.offerServiceCard.getText().should.eventually.to.be.equal(
                    'Установка1',
                    'Выбранная услуга сохраняется после обновления страницы'
                );

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
    },
});
