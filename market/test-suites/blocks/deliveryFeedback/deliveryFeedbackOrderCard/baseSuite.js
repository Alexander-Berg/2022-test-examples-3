import {
    makeSuite,
    mergeSuites,
    prepareSuite,
    makeCase,
} from 'ginny';
import {setupOrder, openEntryPage} from '../utils';
import surveySuite from './survey';

module.exports = makeSuite('Опрос для DSBS заказов', {
    feature: 'Виджет "Заказ у меня"',
    environment: 'kadavr',
    params: {
        pageId: 'Идентификатор страницы',
    },
    story: {
        'В статусе DELIVERED.': mergeSuites(
            {
                async beforeEach() {
                    const orderData = await setupOrder(this, {status: 'DELIVERED', substatus: 'DELIVERY_SERVICE_DELIVERED'});

                    this.params.orderData = orderData;

                    await openEntryPage(this, {orderId: orderData.id});

                    await this.widget.waitForVisible();
                },
            },
            {
                'Отображение виджета': makeCase({
                    id: 'bluemarket-3714',
                    issue: 'MARKETFRONT-16609',
                    async test() {
                        await this.widget.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Виджет отображается'
                            );
                    },
                }),
            },
            prepareSuite(surveySuite)
        ),
        'В статусе DELIVERY, c просроченной доставкой.': mergeSuites(
            {
                async beforeEach() {
                    const orderData = await setupOrder(this, {status: 'DELIVERY', substatus: 'DELIVERY_SERVICE_RECEIVED', deliveryDaysDiff: 3});

                    this.params.orderData = orderData;

                    await openEntryPage(this, {orderId: orderData.id});

                    await this.widget.waitForVisible();
                },
            },
            {
                'Отображение виджета.': makeCase({
                    id: 'bluemarket-3714',
                    issue: 'MARKETFRONT-16609',
                    async test() {
                        await this.widget.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Виджет отображается'
                            );
                    },
                }),
            },
            prepareSuite(surveySuite)
        ),
    },
});

