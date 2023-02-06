import {makeSuite, makeCase} from 'ginny';

import OrderGrade from '@self/root/src/components/OrderCardGrade/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import yaBuildURL from '@self/root/src/spec/hermione/commands/yaBuildURL';
import RatingControl from '@self/root/src/uikit/components/RatingControl/__pageObject';
import assert from 'assert';
import OrderFeedbackPage from './page/';
import {ORDER_ID, setupFeedback, setupTest} from './utils/common';


export default makeSuite('Компонент оценки заказа в карточке заказа', {
    environment: 'kadavr',
    feature: 'Оценка заказа',
    story: {
        async beforeEach() {
            assert(this.params.pageId, 'Param pageId must be defined');
            assert(this.orderCard, 'PageObject.orderCard must be defined');

            this.setPageObjects({
                orderCardGrade: () => this.createPageObject(
                    OrderGrade,
                    {parent: this.orderCard}
                ),
                ratingControl: () => this.createPageObject(
                    RatingControl,
                    {parent: this.orderCardGrade}
                ),
                orderFeedbackPage: () => this.createPageObject(OrderFeedbackPage),
            });
        },

        'На доставленном и неоценённом заказе отображаются звезды': makeCase({
            id: 'bluemarket-3502',
            issue: 'BLUEMARKET-10940',
            async test() {
                await setupTest(this, {pageParams: {orderId: ORDER_ID}});
                await this.ratingControl.clickOnGrade(4);
                await this.orderFeedbackPage.waitForPageOpened();

                await this.browser.getUrl()
                    .should.eventually.be.link({
                        pathname: yaBuildURL(PAGE_IDS_COMMON.ORDER_FEEDBACK, {orderId: ORDER_ID}),
                        query: {grade: '4'},
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                        skipPathname: false,
                    });
            },
        }),

        'На заказ с оценкой отображается приглашение оставить отзыв': makeSuite('', {
            story: {
                async beforeEach() {
                    await setupFeedback(this, {grade: 4});
                    await setupTest(this, {pageParams: {orderId: ORDER_ID}});
                },
                'Жмем на кнопку "оценить"': makeCase({
                    id: 'bluemarket-3503',
                    issue: 'BLUEMARKET-10940',
                    async test() {
                        await this.orderCardGrade.clickOnSendFeedback();
                        await this.orderFeedbackPage.waitForPageOpened();
                    },
                }),
                'Жмем на кнопку "пропустить"': makeCase({
                    id: 'bluemarket-3503',
                    issue: 'BLUEMARKET-10940',
                    async test() {
                        await this.orderCardGrade.clickOnSkipFeedback();
                        await this.orderCardGrade.waitForVisible(true)
                            .should.eventually.be.equal(true, 'Ожидаем скрытия');
                        await this.browser.yaPageReloadWithWaiters();
                        await this.orderCardGrade.waitForVisible(true);
                    },
                }),
            },
        }),
    },
});
