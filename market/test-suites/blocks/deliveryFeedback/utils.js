import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import dayjs from 'dayjs';
import assert from 'assert';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import AvailableSupportChannels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/mp_available_support_channels';

export const ORDER_ID = '42';
export const KETTLE_PICTURE = '//avatars.mds.yandex.net/get-mpic/466729/img_id3389815133238398301.jpeg/';

export async function openEntryPage(ctx, {orderId} = {}) {
    assert(ctx.params.pageId, 'Param pageId must be defined');

    /**
     * CMS-мок нужен, так как управление доступностью чата
     * у нас происходит через CMS
     */
    await ctx.browser.setState(
        'Tarantino.data.result',
        [AvailableSupportChannels]
    );

    await ctx.browser.yaSetCookie({
        name: 'autotest_hide_chat',
        value: '0',
        path: '/',
    });

    if (ctx.params.pageId === PAGE_IDS_COMMON.ORDER) {
        return ctx.browser.yaOpenPage(ctx.params.pageId);
    }

    if (ctx.params.pageId === PAGE_IDS_COMMON.ORDERS) {
        return ctx.browser.yaOpenPage(ctx.params.pageId, {orderId});
    }

    if (ctx.params.pageId === PAGE_IDS_COMMON.INDEX) {
        return ctx.browser.yaOpenPage(ctx.params.pageId, {mock: '1'});
    }
}

export function validateOrderDataParams(orderData) {
    assert(orderData, 'Param orderData must be defined in test context');
    assert(orderData.id, 'Param orderData.id must be defined in test context');
    assert(orderData.orderItemImage, 'Param orderData.orderItemImage must be defined in test context');
}

export async function setupOrder(ctx, {status, substatus, deliveryDaysDiff = 0} = {}) {
    assert(status, 'Param status must be defined');
    assert(substatus, 'Param substatus must be defined');

    const deliveryToDate = dayjs()
        .subtract(deliveryDaysDiff, 'day')
        .format('DD-MM-YYYY');

    const orderData = {
        substatus,
        status,
        paymentType: 'PREPAID',
        paymentMethod: 'YANDEX',
        fulfilment: false,
        rgb: 'WHITE',
        region: ctx.params.region,
        orders: [{
            id: ORDER_ID,
            items: [{
                skuMock: kettle.skuMock,
                offerMock: kettle.offerMock,
                pictures: [{
                    url: KETTLE_PICTURE,
                }],
                count: 1,
            }],
            deliveryType: 'DELIVERY',
            delivery: {
                deliveryPartnerType: 'SHOP',
                userReceived: false,
                dates: {
                    fromDate: deliveryToDate,
                    toDate: deliveryToDate,
                },
            },
        }],
    };

    await ctx.browser.yaScenario(
        ctx,
        prepareOrder,
        orderData
    );

    return {
        id: ORDER_ID,
        orderItemImage: KETTLE_PICTURE,
    };
}
