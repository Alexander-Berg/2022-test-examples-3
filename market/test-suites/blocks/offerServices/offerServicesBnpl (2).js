import {makeSuite, makeCase} from 'ginny';

import BnplInfo from '@self/root/src/components/BnplInfo/__pageObject';
import BnplSwitch from '@self/root/src/components/BnplSwitch/__pageObject';
import {NON_SELECTED_TEXT} from '@self/root/src/components/OfferServiceCard';
import OfferServiceCard from '@self/root/src/components/OfferServiceCard/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {offerKettleBnplServices, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareCartState} from '@self/root/src/spec/hermione/scenarios/bnpl';
import SelectedMounting
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/OfferServices/Mounting/components/SelectedMounting/__pageObject';
import CheckoutLayoutConfirmation
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import {
    DEFAULT_OFFER_WARE_ID,
    productWithCPADefaultOfferAndServicesBnpl,
} from '@self/platform/spec/hermione/fixtures/product';
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';

export default makeSuite('БНПЛ', {
    id: 'MARKETFRONT-70036',
    environment: 'kadavr',
    feature: 'Доп услуги с рассрочкой',
    issue: 'MARKETFRONT-57701',
    defaultParams: {
        item: {
            sku: skuMock,
            offer: offerKettleBnplServices,
            count: 1,
        },
        firstOrder: false,
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                offerSummary: () => this.createPageObject(OfferSummary),
                offerServiceCard: () => this.createPageObject(OfferServiceCard),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
                bnplInfo: () => this.createPageObject(BnplInfo),
                confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                bnplSwitch: () => this.createPageObject(BnplSwitch),
                selectedMounting: () => this.createPageObject(SelectedMounting),
            });

            await this.browser.setState('report', productWithCPADefaultOfferAndServicesBnpl);

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.OFFER, {offerId: DEFAULT_OFFER_WARE_ID});
            await this.browser.allure.runStep(
                'Дожидаемся загрузки блока с главной инофрмацией об оффере',
                () => this.offerSummary.waitForVisible()
            );

            await this.browser.yaScenario(this, prepareCartState);
        },

        'Сохранение выбранной услуги при оформлении рассрочки': makeCase({
            async test() {
                await this.offerServiceCard.waitForRootVisible(1000);
                await this.offerServiceCard.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга изначально не выбрана');

                await this.offerServiceCard.addButtonClick();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();
                await this.bnplInfo.button.click();

                await this.confirmationPage.waitForVisible(5000);

                await this.bnplSwitch.isVisible().should.eventually.be.equal(
                    true,
                    'Переключатель bnpl должен быть виден'
                );
                await this.bnplSwitch.checkbox.getAttribute('checked').should.eventually.be.equal(
                    'true',
                    'Переключатель bnpl должен быть включен'
                );
                await this.selectedMounting.waitForRootVisible(1000);
                await this.selectedMounting.getText().should.eventually.be.equal(
                    'Установка1',
                    'Услуга сохранилась при переходе в чекаут'
                );
            },
        }),
    },
});
