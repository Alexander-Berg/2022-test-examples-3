import {
    makeSuite,
    makeCase,
} from 'ginny';
import dayjs from 'dayjs';
import {replaceBreakChars} from '@self/root/src/spec/utils/text';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {mergeState, createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import returnOptionsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import returnOutletsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

import {
    ReasonInPlacePanel,
} from '@self/root/src/widgets/parts/ReturnCandidate/components/RestrictedUnfitReasonInfo/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import RETURN_TEXTS from '@self/root/src/widgets/parts/ReturnCandidate/constants/i18n';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';


const ID = 11111;
const TOUCH_INDEX = 1;

export default makeSuite('Ограничение на возврат товаров надлежащего качества через 7 дней.', {
    environment: 'kadavr',
    params: {
        items: 'Товары',
        paymentType: 'Тип оплаты',
    },
    defaultParams: {
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            count: 1,
            id: ID,
        }],
    },
    feature: 'Ограничение на возврат товаров надлежащего качества.',
    issue: 'MARKETFRONT-19415',
    story: {
        async beforeEach() {
            this.setPageObjects({
                returnsPage: () => this.createPageObject(ReturnsPage),
                returnItemsScreen: () => this.createPageObject(
                    ReturnItems,
                    {parent: this.returnsPage}
                ),
                reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsPage}),
                returnItemUnfitReasonWarning: () => this.createPageObject(ReasonInPlacePanel, {parent: this.reasonsChooseScreen}),
            });

            const shopId = 101;
            const shopInfo = createShopInfo({
                returnDeliveryAddress: 'hello, there!',
            }, shopId);

            await this.browser.yaScenario(this, setReportState, {
                state: mergeState(
                    [shopInfo],
                    {
                        data: {
                            results: returnOutletsMock,
                            search: {results: []},
                        },
                    }
                ),
            });

            await this.browser.setState(
                'Checkouter.returnOptions',
                returnOptionsMock
            );

            await this.browser.setState(
                'Checkouter.returnableItems',
                this.params.items.map(item => ({
                    ...item,
                    itemId: item.id,
                }))
            );

            return this.browser.yaScenario(this, 'checkoutResource.prepareOrder', {
                region: this.params.region,
                orders: [{
                    items: this.params.items,
                    deliveryType: 'DELIVERY',
                    shopId,
                }],
                paymentType: this.params.paymentType,
                paymentMethod: this.params.paymentType === 'PREPAID' ? 'YANDEX' : 'CASH_ON_DELIVERY',
                status: 'DELIVERED',
                statusUpdateDate: dayjs().subtract(7, 'day').subtract(10, 'minute').toDate(),
            })
                .then(result => this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.CREATE_RETURN, {
                    orderId: result.orders[0].id,
                    type: 'refund',
                }));
        },

        'При превышении времени на возврат товара надлежащего качества': {
            'Показывается уведомление при выборе причины "не подошел"': makeCase({
                async test() {
                    await this.reasonsChooseScreen.setReasonForItem(TOUCH_INDEX, 'do_not_fit');

                    await this.returnItemUnfitReasonWarning.isVisible()
                        .should.eventually.to.equal(
                            true,
                            'Уведомление о превышении времени на возврат товара надлежащего качества отображается'
                        );

                    await this.returnItemUnfitReasonWarning.getTitleText()
                        .should.eventually.to.equal(
                            replaceBreakChars(RETURN_TEXTS.REASON_UNFIT_REASON_RESTRICTED_TITLE),
                            'В уведомлении отображается правильный заголовок'
                        );

                    await this.returnItemUnfitReasonWarning.getDescriptionText()
                        .should.eventually.to.equal(
                            replaceBreakChars(RETURN_TEXTS.REASON_UNFIT_REASON_RESTRICTED_DESCRIPTION),
                            'В уведомлении отображается правильное описание'
                        );

                    await this.reasonsChooseScreen.isButtonClickable()
                        .should.eventually.to.equal(
                            false,
                            'Кнопка "Продолжить" заблокирована'
                        );
                },
            }),
        },
    },
});
