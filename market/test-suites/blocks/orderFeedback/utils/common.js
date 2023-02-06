import dayjs from 'dayjs';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {setOrderFeedbackState} from '@self/root/src/spec/hermione/scenarios/kadavr';

export const ORDER_ID = 42;

const KETTLE_PICTURE = '//avatars.mds.yandex.net/get-mpic/466729/img_id3389815133238398301.jpeg/';
export const simpleOrderItem = {
    skuMock: kettle.skuMock,
    offerMock: kettle.offerMock,
    pictures: [{
        url: KETTLE_PICTURE,
    }],
    count: 1,
};

const getDateByDaysFromNow = dayDiff =>
    dayjs()
        .subtract(dayDiff, 'day')
        .format('DD-MM-YYYY');

export async function setupOrder(ctx, {daysFromNow = 1, status = 'DELIVERED'} = {}) {
    await ctx.browser.yaScenario(ctx, prepareOrder, {
        region: 213,
        orders: [{
            orderId: 42,
            items: [simpleOrderItem],
            deliveryType: 'DELIVERY',
            delivery: {
                dates: {
                    toDate: getDateByDaysFromNow(daysFromNow),
                },
            },
        }],
        paymentType: 'PREPAID',
        paymentMethod: 'YANDEX',
        status: 'DELIVERED',
    });
}

export async function setupFeedback(ctx, feedback = {}) {
    await ctx.browser.yaScenario(ctx, setOrderFeedbackState, {
        state: {
            feedback: {
                [ORDER_ID]: {
                    orderId: ORDER_ID,
                    ...feedback,
                },
            },
        },
    });
}

export async function setupTest(ctx, {order = {}, pageParams = {}} = {}) {
    await setupOrder(ctx, order);
    await ctx.browser.yaOpenPage(ctx.params.pageId, {...pageParams});
}

