import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import dayjs from 'dayjs';
import assert from 'assert';
import {
    mockOrderConsultationsState,
} from '@self/root/src/spec/hermione/scenarios/orderConsultation';
import AvailableSupportChannels from '@self/root/src/spec/hermione/kadavr-mock/tarantino/mp_available_support_channels';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';

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
}

export async function setupOrderConsultations(ctx, {isExisting, orderId}) {
    assert(isExisting !== undefined, 'Param isExisting must be defined');

    const orderConsultations = isExisting
        ? [
            {
                orderNumber: orderId,
                chatId: '0/25/fb1cf15e-4b77-42b6-8808-3cd521b788c2',
                consultationStatus: 'DIRECT_CONVERSATION',
                conversationStatus: 'WAITING_FOR_CLIENT',
            },
        ]
        : [];

    await ctx.browser.yaScenario(
        ctx,
        mockOrderConsultationsState,
        {orderConsultations}
    );
}

export async function setupOrder(ctx, {status, substatus, isDsbs} = {}) {
    assert(status, 'Param status must be defined');
    assert(isDsbs !== undefined, 'Param isDsbs must be defined');

    const deliveryToDate = dayjs()
        .format('DD-MM-YYYY');

    const orderData = {
        status,
        substatus: substatus || 'UNKNOWN',
        paymentType: 'PREPAID',
        paymentMethod: 'YANDEX',
        fulfilment: false,
        rgb: isDsbs ? 'WHITE' : 'BLUE',
        region: ctx.params.region,
        orders: [{
            items: [{
                skuMock: kettle.skuMock,
                offerMock: kettle.offerMock,
                pictures: [{
                    url: KETTLE_PICTURE,
                }],
                count: 1,
            }],
            deliveryType: status === 'PICKUP' ? status : 'DELIVERY',
            outletId: '66843529',
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

    const checkoutResult = await ctx.browser.yaScenario(
        ctx,
        prepareOrder,
        orderData
    );

    return {
        orderId: checkoutResult.orders[0].id,
    };
}
