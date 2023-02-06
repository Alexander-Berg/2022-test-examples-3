import {makeSuite, makeCase} from 'ginny';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {
    SummaryPlaceholder,
} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {waitForCartActualization} from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {getExpressCart} from '@self/root/src/spec/hermione/test-suites/blocks/cart/groupedByTreshold/helpers';
import CartCheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartLayout from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import OfferServiceText
    from '@self/root/src/widgets/content/cart/CartList/components/OfferServiceText/__pageObject';
import {
    NON_SELECTED_TEXT,
} from '@self/root/src/widgets/content/cart/CartList/components/OfferServiceText/constants';
import CartTotalInformation
    from '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import LoginAgitation
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/LoginAgitation/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import Clickable from '@self/root/src/components/Clickable/__pageObject';

import params from './params';

const getTotalPriceAmount = total => Number.parseInt(total.split('\n')[1].replace(/\s/, ''), 10);

export default makeSuite('Корзина', {
    id: 'MARKETFRONT-57697',
    environment: 'kadavr',
    feature: 'Доп услуги на КМ',
    issue: 'MARKETFRONT-57693',
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartGroup: () => this.createPageObject(CartLayout),
                orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                orderTotal: () => this.createPageObject(OrderTotal),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                offerServiceText: () => this.createPageObject(OfferServiceText),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
                loginAgitationWrapper: () => this.createPageObject(LoginAgitation),
                agitationModal: () => this.createPageObject(PopupBase, {
                    parent: this.loginAgitationWrapper,
                }),
                notNowButton: () => this.createPageObject(Clickable, {
                    parent: this.agitationModal,
                    root: '[data-autotest-id="notNow"]',
                }),
            });

            await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [getExpressCart({yaPlus: true})]
            );

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART, {lr: 213});
            await this.browser.yaScenario(this, waitForCartActualization);
        },

        'Добавление услуги в корзине': makeCase({
            async test() {
                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга изначально не выбрана');
                const total = await this.orderTotal.getTotalText();
                const totalAmount = getTotalPriceAmount(total);

                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана');
                const newTotal = await this.orderTotal.getTotalText();
                const newTotalAmount = getTotalPriceAmount(newTotal);
                return this.expect(newTotalAmount)
                    .to.be.equal(totalAmount + 42, 'Стоимость услуги учтена в сумме заказа');
            },
        }),

        'Изменение услуг в корзине': makeCase({
            async test() {
                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана');
                const total = await this.orderTotal.getTotalText();
                const totalAmount = getTotalPriceAmount(total);

                await this.browser.refresh();
                await this.browser.yaScenario(this, waitForCartActualization);

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана после обновления страницы');

                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService(2);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка2', 'Выбрана новая услуга');
                const newTotal = await this.orderTotal.getTotalText();
                const newTotalAmount = getTotalPriceAmount(newTotal);
                return this.expect(newTotalAmount)
                    .to.be.equal(totalAmount + (43 - 42), 'Стоимость новой услуги учтена в сумме заказа');
            },
        }),

        'Удаление услуги в корзине': makeCase({
            async test() {
                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана');
                const total = await this.orderTotal.getTotalText();
                const totalAmount = getTotalPriceAmount(total);

                await this.browser.refresh();
                await this.browser.yaScenario(this, waitForCartActualization);

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана после обновления страницы');

                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService('none');
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal(NON_SELECTED_TEXT, 'Услуга не выбрана');
                const newTotal = await this.orderTotal.getTotalText();
                const newTotalAmount = getTotalPriceAmount(newTotal);
                return this.expect(newTotalAmount)
                    .to.be.equal(totalAmount - 42, 'Стоимость услуги не учитывается в сумме заказа');
            },
        }),

        'Возможен переход в чекаут из корзины с услугой': makeCase({
            async test() {
                await this.offerServiceText.change();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                await this.offerServiceText.getText()
                    .should.eventually.to.be.equal('Установка1', 'Услуга выбрана');

                await this.cartCheckoutButton.goToCheckout();
                if (params.hasAgitation && !this.params.isAuthWithPlugin) {
                    await this.agitationModal.waitForVisible();
                    await this.browser.yaWaitForChangeUrl(() => this.notNowButton.click());
                }

                const [openedUrl, expectedPath] = await Promise.all([
                    this.browser.getUrl(),
                    this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT),
                ]);
                await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                    skipProtocol: true,
                    skipHostname: true,
                });
            },
        }),
    },
});
