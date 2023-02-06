import {
    makeSuite,
    makeCase,
} from 'ginny';


import OutletInfo from '@self/root/src/widgets/content/OutletInfo/components/View/__pageObject';
import OutletInfoContent from '@self/root/src/widgets/content/OutletInfo/components/OutletInfoContent/__pageObject';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import OutletActions from '@self/root/src/widgets/content/OutletInfo/components/OutletActions/__pageObject';

import {getOutletPaymentMethods} from '@self/root/src/entities/outlet/getters';
import {getPaymentMethodTitle} from '@self/root/src/entities/payment/getters';
import {getPrettyFormattedPhone} from '@self/root/src/entities/phone/getters';

import {SECTION_TITLE} from '@self/root/src/widgets/content/OutletInfo/components/OutletInfoContent/constants';

import {
    nonBrandedOutletMock,
    checkActionBlockVisibility,
    getDeliveryConditionsCorrectText,
} from '../helpers';

export default makeSuite('По умолчанию', {
    id: 'bluemarket-3937',
    story: {
        beforeEach() {
            this.setPageObjects({
                outletInfo: () => this.createPageObject(OutletInfo),
                outletInfoContent: () => this.createPageObject(OutletInfoContent),
                secondaryButton: () => this.createPageObject(Button, {
                    parent: OutletActions.secondaryButton,
                }),
            });
        },

        Заголовок: {
            'отображается корректно': makeCase({
                async test() {
                    return this.outletInfo.getTitle()
                        .should.eventually.be.equal(
                            nonBrandedOutletMock.name,
                            'Заголовок должен соответствовать названию ПВЗ'
                        );
                },
            }),
        },

        Адрес: {
            'отображается корректно': makeCase({
                async test() {
                    return this.outletInfoContent.getAddress()
                        .should.eventually.be.equal(
                            nonBrandedOutletMock.address.fullAddress,
                            'Адрес должен соответствовать адресу ПВЗ'
                        );
                },
            }),
        },

        'Расписание работы': {
            'отображается корректно': makeCase({
                async test() {
                    return this.outletInfoContent.isBusinessScheduleVisible()
                        .should.eventually.be.equal(
                            true,
                            'Расписание работы ПВЗ должно отображаться'
                        );
                },
            }),
        },

        'Блок "Как добраться"': {
            'отображается корректно': makeCase({
                async test() {
                    await this.outletInfoContent.isAddressNoteVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок "Как добраться" должен быть виден'
                        );

                    await this.outletInfoContent.getAddressNoteTitle()
                        .should.eventually.be.equal(
                            SECTION_TITLE.OUTLET_ADDRESS_NOTE,
                            'Заголовок блока "Как добраться" должен быть корректным'
                        );

                    return this.outletInfoContent.getAddressNoteText()
                        .should.eventually.be.equal(
                            nonBrandedOutletMock.address.note,
                            'Текст блока "Как добраться" должен быть корректным'
                        );
                },
            }),
        },

        'Блок со стоимостью доставки': {
            'отображается корректно': makeCase({
                async test() {
                    await this.outletInfoContent.areDeliveryConditionsVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок со стоимостью доставки должен быть виден'
                        );

                    await this.outletInfoContent.getDeliveryConditionsTitle()
                        .should.eventually.be.equal(
                            SECTION_TITLE.OUTLET_DELIVERY_CONDITIONS,
                            'Заголовок блока со стоимостью доставки должен быть корректным'
                        );

                    return this.outletInfoContent.getDeliveryConditionsText()
                        .should.eventually.be.equal(
                            getDeliveryConditionsCorrectText(),
                            'Текст блока со стоимостью доставки должен быть корректным'
                        );
                },
            }),
        },

        'Блок со способами оплаты': {
            'отображается корректно': makeCase({
                async test() {
                    await this.outletInfoContent.isPaymentInformationVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок со способами оплаты должен быть виден'
                        );

                    await this.outletInfoContent.getPaymentInformationTitle()
                        .should.eventually.be.equal(
                            SECTION_TITLE.OUTLET_PAYMENT_INFORMATION,
                            'Заголовок блока со способами оплаты должен быть корректным'
                        );

                    const paymentMethods = getOutletPaymentMethods({
                        booleanProperties: nonBrandedOutletMock.BooleanProperties,
                    });

                    const paymentMethodTexts = paymentMethods.map(paymentMethod => getPaymentMethodTitle({paymentMethod}));

                    return this.outletInfoContent.getPaymentInformationText()
                        .should.eventually.be.equal(
                            paymentMethodTexts.join('\n'),
                            'Текст блока со способами оплаты должен быть корректным'
                        );
                },
            }),
        },

        'Блок с контактами': {
            'отображается корректно': makeCase({
                async test() {
                    await this.outletInfoContent.areContactsVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок с контактами должен быть виден'
                        );

                    await this.outletInfoContent.getContactsTitle()
                        .should.eventually.be.equal(
                            SECTION_TITLE.OUTLET_CONTACTS,
                            'Заголовок блока с контактами должен быть корректным'
                        );

                    const outletPhone = nonBrandedOutletMock.telephones[0];

                    const phone = {
                        countryCode: outletPhone.countryCode,
                        regionCode: outletPhone.cityCode,
                        localNumber: outletPhone.telephoneNumber,
                        extensionNumber: outletPhone.extensionNumber,
                    };

                    return this.outletInfoContent.getContactsText()
                        .should.eventually.be.equal(
                            getPrettyFormattedPhone(phone),
                            'Текст блока с контактами должен быть корректным'
                        );
                },
            }),
        },

        Кнопки: {
            'отображаются и работают корректно': makeCase({
                async test() {
                    await checkActionBlockVisibility.call(this);

                    /**
                     * Здесь проверяем только видимость кнопки,
                     * так как текст проверяется в других тестах
                     */
                    await this.primaryButton.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка добавления в избранное должна быть видна'
                        );

                    await this.secondaryButton.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка "К покупкам" должна быть видна'
                        );

                    await this.secondaryButton.getButtonText()
                        .should.eventually.be.equal(
                            'К покупкам',
                            'Текст кнопки "К покупкам" должен быть корректным'
                        );

                    const url = await this.allure.runStep('Кликаем по кнопке "К покупкам"', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.secondaryButton.click()
                        )
                    );

                    return this.allure.runStep('Проверяем, что произошёл переход на главную страницу', () => (
                        this.expect(url).to.be.link({
                            pathname: '/',
                        }, {
                            skipHostname: true,
                            skipProtocol: true,
                        })
                    ));
                },
            }),
        },
    },
});
