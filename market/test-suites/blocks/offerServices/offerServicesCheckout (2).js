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
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/touch/goToConfirmationPage';
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
import DeliveryEditor
    from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/View/__pageObject';
import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import Checkout2Page from '@self/root/src/widgets/pages.touch/Checkout2Page/__pageObject';
import {moscowAddress} from '@self/platform/spec/hermione/test-suites/blocks/checkout2/hsch/differentRegions/mocks';

const getTotalPriceAmount = total => Number.parseInt(total.split('\n')[1].replace(/\s/, ''), 10);
const getDateOrTimeText = text => text.split('\n')[1];

export default makeSuite('????????????', {
    id: 'MARKETFRONT-57699',
    environment: 'kadavr',
    feature: '?????? ???????????? ?? ??????????????',
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
                checkoutPage: () => this.createPageObject(Checkout2Page),
                deliveryEditor: () => this.createPageObject(DeliveryEditor),

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

            await this.browser.yaScenario(this, goToConfirmationPage, {
                userFormData,
                addressFormData: moscowAddress,
            });
        },

        '?????????????????? ???????????? ?? ??????????????': makeCase({
            async test() {
                const total = await this.orderTotal.getTotalText();
                const totalAmount = getTotalPriceAmount(total);

                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                const newTotal = await this.orderTotal.getTotalText();
                const newTotalAmount = getTotalPriceAmount(newTotal);
                return this.expect(newTotalAmount)
                    .to.be.equal(totalAmount + 42, '?????????????????? ???????????? ???????????? ?? ?????????? ????????????');
            },
        }),

        '?????????????????? ???????? ?? ?????????????? ?? ??????????????': makeCase({
            async test() {
                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                await this.expect(this.mountingDateSelectButton.getText())
                    .to.be.equal('??????????????????????, 25 ??????????????', '?????????????? ???????? ????-??????????????????');
                await this.expect(this.mountingTimeSelectButton.getText())
                    .to.be.equal('?? 08:00 ???? 14:00', '?????????????? ?????????? ????-??????????????????');

                await this.mountingDateSelectButton.click();
                const newDate = '??????????????????????, 26 ??????????????';
                await this.selectPopover.clickOptionByText(newDate);
                await this.selectPopover.waitForListIsInvisible();

                await this.expect(this.mountingDateSelectButton.getText())
                    .to.be.equal(newDate, '???????? ???????????? ????????????????????');

                await this.mountingTimeSelectButton.click();
                const newTime = '?? 14:00 ???? 20:00';
                await this.selectPopover.clickOptionByText(newTime);
                await this.selectPopover.waitForListIsInvisible();

                return this.expect(this.mountingTimeSelectButton.getText())
                    .to.be.equal(newTime, '?????????? ???????????? ????????????????????');
            },
        }),

        '?????????????????? ?? ???????? ????????????': makeCase({
            async test() {
                await this.selectedMounting.changeService();
                await this.popupChoosingService.chooseService(1);
                await this.popupChoosingService.saveButtonClick();

                let deliveryDate = getDateOrTimeText(await this.deliveryDateSelect.getText());
                await this.expect(deliveryDate).to.be.equal('????, 24 ??????????????, 250 ???', '?????????????? ???????? ????????????????');
                await this.expect(this.mountingDateSelectButton.getText())
                    .to.be.equal('??????????????????????, 25 ??????????????', '?????????????? ???????? ??????????????????');
                await this.expect(this.mountingTimeSelectButton.getText())
                    .to.be.equal('?? 08:00 ???? 14:00', '?????????????? ?????????? ??????????????????');

                await this.mountingDateSelectButton.click();
                await this.selectPopover.clickOptionByText('??????????????, 24 ??????????????');
                await this.changeDeliveryDatePopup.saveButtonClick();

                await this.expect(this.mountingDateSelectButton.getText())
                    .to.be.equal('??????????????, 24 ??????????????', '???????? ???????????? ????????????????????');
                deliveryDate = getDateOrTimeText(await this.deliveryDateSelect.getText());
                await this.expect(deliveryDate)
                    .to.be.equal('????, 24 ??????????????, 250 ???', '???????? ???????????????? ?????????????????? ?? ?????????? ??????????????????');
                return this.expect(this.mountingTimeIntervals.isVisible())
                    .to.be.equal(false, '?????????? ?????????????? ???????????? ???? ????????????????');
            },
        }),
    },
});
