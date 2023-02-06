import {
    makeSuite,
    makeCase,
} from 'ginny';

// pageObjects
import ConfirmationPage from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.desktop.js';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import CheckoutRecipient from
    '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutSummary from '@self/root/src/components/CheckoutSummary/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotal/__pageObject';
import CheckoutOrderButton from
    '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import CheckoutSuppliersInfo from
    '@self/root/src/widgets/content/checkout/common/CheckoutSuppliersInfo/components/View/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';

export default makeSuite('Основные данные.', {
    environment: 'kadavr',
    params: {
        userInfo: 'Данные пользователя',
        pageTitle: 'Заголовок страницы',
        parcelTitle: 'Заголовок посылки',
        selectedAddress: 'Выбранный адрес',
        shop: 'Поставщик',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                confirmationPage: () => this.createPageObject(ConfirmationPage),
                groupedParcels: () => this.createPageObject(GroupedParcels, {
                    parent: this.confirmationPage,
                }),
                groupedParcel: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                }),
                editableCard: () => this.createPageObject(EditableCard, {
                    parent: this.confirmationPage,
                }),
                recipientCardWidget: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                recipientCard: () => this.createPageObject(EditableCard, {
                    root: `${CheckoutRecipient.root}${EditableCard.root}`,
                    parent: this.confirmationPage,
                }),
                editPayment: () => this.createPageObject(EditableCard, {
                    root: `${EditPaymentOption.root} ${EditableCard.root}`,
                    parent: this.confirmationPage,
                }),
                editPaymentContent: () => this.createPageObject(EditPaymentOption, {
                    root: EditPaymentOption.content,
                    parent: this.confirmationPage,
                }),
                summary: () => this.createPageObject(CheckoutSummary, {
                    parent: this.confirmationPage,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: this.summary,
                }),
                checkoutOrderButton: () => this.createPageObject(Button, {
                    root: `${CheckoutOrderButton.root} ${Button.root}`,
                    parent: this.confirmationPage,
                }),
                checkoutSuppliersInfo: () => this.createPageObject(CheckoutSuppliersInfo, {
                    parent: this.confirmationPage,
                }),
            });
        },

        'Блок получателя.': makeCase({
            async test() {
                const {
                    userInfo,
                } = this.params;

                const EXPECTED_TITLE_TAG = 'h2';

                await this.recipientCard.getTitleTag()
                    .should.eventually.to.be.equal(
                        EXPECTED_TITLE_TAG,
                        `На карточке получателя заголовок должен быть тэгом ${EXPECTED_TITLE_TAG}`
                    );

                await this.recipientCard.getTitle()
                    .should.eventually.to.be.equal(
                        'Получатель',
                        'На карточке получателя заголовок "Получатель"'
                    );

                await this.recipientCardWidget.getContactText()
                    .should.eventually.to.be.equal(
                        `${userInfo.name}\n${userInfo.email}, ${userInfo.phone}`,
                        'На карточке получателя должны быть указанные пользователем данные'
                    );
            },
        }),

        'Блок оплаты.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Заголовок должен быть правильным.',
                    async () => {
                        const {
                            pageTitle,
                        } = this.params;
                        const EXPECTED_TITLE_TAG = 'h1';

                        await this.confirmationPage.getTitleTag()
                            .should.eventually.to.be.equal(
                                EXPECTED_TITLE_TAG,
                                `Тэг заголовка должен быть ${EXPECTED_TITLE_TAG}`
                            );

                        await this.confirmationPage.getTitle()
                            .should.eventually.to.be.equal(
                                pageTitle,
                                `Заголовок страницы должен быть "${pageTitle}"`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Способы оплаты.',
                    async () => {
                        const EXPECTED_TITLE_TAG = 'h2';

                        await this.editPayment.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Карточка способа оплаты должна быть видна'
                            );

                        await this.editPayment.getTitleTag()
                            .should.eventually.to.be.equal(
                                EXPECTED_TITLE_TAG,
                                `На карточке способа оплаты заголовок должен быть ${EXPECTED_TITLE_TAG}`
                            );

                        await this.editPayment.getTitle()
                            .should.eventually.to.be.equal(
                                'Способ оплаты',
                                'На карточке способа оплаты заголовок "Способ оплаты"'
                            );

                        await this.editPaymentContent.getText()
                            .should.eventually.to.be.match(
                                new RegExp('(Новой картой|••••\\s\\s\\d{4})$'),
                                'Должен быть выбран способ оплаты картой'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок резюме по заказу.',
                    async () => {
                        await this.summary.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Блок "ваш заказ" должен быть виден'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка оформить.',
                    async () => {
                        await this.checkoutOrderButton.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка перейти к оплате должна быть видна'
                            );

                        await this.checkoutOrderButton.getButtonText()
                            .should.eventually.to.be.match(
                                new RegExp('(Оплатить картой|Перейти к оплате)$'),
                                'На кнопке должна быть надпись "Перейти к оплате"'
                            );

                        await this.checkoutOrderButton.isDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Перейти к оплате" не должна быть заблокирована'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок под кнопкой оформить.',
                    async () => {
                        await this.checkoutSuppliersInfo.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Блок о поставщике должен быть виден'
                            );

                        await this.checkoutSuppliersInfo.getText()
                            .should.eventually.to.be.equal(
                                'О товаре и продавце',
                                'Блок о поставщике должен содержать правильный текст'
                            );
                    }
                );
            },
        }),
    },
});
