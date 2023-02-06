import {makeSuite, makeCase} from 'ginny';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import PopupChoosingService from '@self/root/src/components/PopupChoosingService/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {SelectButton, SelectPopover} from '@self/root/src/components/Select/__pageObject';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import * as offerServices from '@self/root/src/spec/hermione/kadavr-mock/report/offerServices';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    DateSelect as DeliveryDateSelect,
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DateIntervalSelector/__pageObject';
import ChangeDeliveryDatePopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/OfferServices/Mounting/components/ChangeDeliveryDatePopup/__pageObject';
import {
    DateSelect,
    TimeSelect,
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/OfferServices/Mounting/components/MountingIntervals/__pageObject';
import SelectedMounting
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/OfferServices/Mounting/components/SelectedMounting/__pageObject';
import CheckoutSummary
    from '@self/root/src/components/CheckoutSummary/__pageObject';
import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';

const getTotalPriceAmount = total => Number.parseInt(total.split('\n')[1].replace(/\s/, ''), 10);
const getDateOrTimeText = text => text.split('\n')[1];

export default makeSuite('Чекаут', {
    id: 'MARKETFRONT-57699',
    environment: 'kadavr',
    feature: 'Доп услуги в чекауте',
    issue: 'MARKETFRONT-57690',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                preloader: () => this.createPageObject(Preloader),
                summaryPlaceholder: () => this.createPageObject(SummaryPlaceholder),
                confirmationPage: () => this.createPageObject(ConfirmationPage),
                summary: () => this.createPageObject(CheckoutSummary, {
                    parent: this.confirmationPage,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: this.summary,
                }),
                deliveryDateSelect: () => this.createPageObject(DeliveryDateSelect),
                selectedMounting: () => this.createPageObject(SelectedMounting),
                mountingDateIntervals: () => this.createPageObject(DateSelect),
                mountingDateSelectButton: () => this.createPageObject(SelectButton, {
                    parent: this.mountingDateIntervals,
                }),
                mountingTimeIntervals: () => this.createPageObject(TimeSelect),
                mountingTimeSelectButton: () => this.createPageObject(SelectButton, {
                    parent: this.mountingTimeIntervals,
                }),
                selectPopover: () => this.createPageObject(SelectPopover),
                popupChoosingService: () => this.createPageObject(PopupChoosingService),
                changeDeliveryDatePopup: () => this.createPageObject(ChangeDeliveryDatePopup),
            });

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: offerServices.skuMock,
                        offerMock: offerServices.offerMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryDeliveryMock,
                        dates: {
                            fromDate: '24-02-2024',
                            toDate: '24-02-2024',
                        },
                        deliveryIntervals: [{
                            intervals: [{
                                fromTime: '10:00',
                                toTime: '14:00',
                            }, {
                                fromTime: '16:00',
                                toTime: '20:00',
                            }],
                        }],
                    }],
                }),
            ];


            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    checkout2: true,
                }
            );

            await this.browser.yaScenario(this, goToConfirmationPage, {userFormData});
        },

        'Изменение услуги в чекауте': makeCase({
            async test() {
                const total = await this.orderTotal.getTotalText();
                const totalAmount = getTotalPriceAmount(total);

                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                const newTotal = await this.orderTotal.getTotalText();
                const newTotalAmount = getTotalPriceAmount(newTotal);
                return this.expect(newTotalAmount)
                    .to.be.equal(totalAmount + 42, 'Стоимость услуги учтена в сумме заказа');
            },
        }),

        'Изменение даты и времени в чекауте': makeCase({
            async test() {
                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                let selectedDate = getDateOrTimeText(await this.mountingDateSelectButton.getText());
                await this.expect(selectedDate).to.be.equal('воскресенье, 25 февраля', 'Выбрана дата по-умолчанию');
                let selectedTime = getDateOrTimeText(await this.mountingTimeSelectButton.getText());
                await this.expect(selectedTime).to.be.equal('с 08:00 до 14:00', 'Выбрано время по-умолчанию');

                await this.mountingDateSelectButton.click();
                const newDate = 'понедельник, 26 февраля';
                await this.selectPopover.clickOptionByText(newDate);
                await this.selectPopover.waitForListIsInvisible();

                selectedDate = getDateOrTimeText(await this.mountingDateSelectButton.getText());
                await this.expect(selectedDate).to.be.equal(newDate, 'Дата услуги изменилась');

                await this.mountingTimeSelectButton.click();
                const newTime = 'с 14:00 до 20:00';
                await this.selectPopover.clickOptionByText(newTime);
                await this.selectPopover.waitForListIsInvisible();

                selectedTime = getDateOrTimeText(await this.mountingTimeSelectButton.getText());
                return this.expect(selectedTime).to.be.equal(newTime, 'Время услуги изменилось');
            },
        }),

        'Установка в день заказа': makeCase({
            async test() {
                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                let deliveryDate = getDateOrTimeText(await this.deliveryDateSelect.getText());
                await this.expect(deliveryDate).to.be.equal('сб, 24 февраля, 250 ₽', 'Выбрана дата доставки');
                let selectedDate = getDateOrTimeText(await this.mountingDateSelectButton.getText());
                await this.expect(selectedDate).to.be.equal('воскресенье, 25 февраля', 'Выбрана дата установки');
                let selectedTime = getDateOrTimeText(await this.mountingTimeSelectButton.getText());
                await this.expect(selectedTime).to.be.equal('с 08:00 до 14:00', 'Выбрано время установки');

                await this.mountingDateSelectButton.click();
                await this.selectPopover.clickOptionByText('суббота, 24 февраля');
                await this.changeDeliveryDatePopup.saveButtonClick();

                selectedDate = getDateOrTimeText(await this.mountingDateSelectButton.getText());
                await this.expect(selectedDate).to.be.equal('суббота, 24 февраля', 'Дата услуги изменилась');
                deliveryDate = getDateOrTimeText(await this.deliveryDateSelect.getText());
                await this.expect(deliveryDate)
                    .to.be.equal('сб, 24 февраля, 250 ₽', 'Дата доставки совпадает с датой установки');
                return this.expect(this.mountingTimeIntervals.isVisible())
                    .to.be.equal(false, 'Выбор времени услуги не доступен');
            },
        }),
    },
});
