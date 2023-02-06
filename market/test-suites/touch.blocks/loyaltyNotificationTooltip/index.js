import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {yaPlusNotification, nonYaPlusNotification} from '@self/root/src/spec/hermione/kadavr-mock/loyaltyNotification';

import LoyaltyNotificationTooltipContentSuite from './loyaltyNotificationTooltipContentSuite';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Уведомление от лоялти.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-50646',
    feature: 'Реферальная программа',
    story: mergeSuites(
        prepareSuite(LoyaltyNotificationTooltipContentSuite, {
            suiteName:
                'Сообщение о применении промокода, ' +
                'при оформлении заказа, ' +
                'для плюсовика.',
            meta: {
                id: 'marketfront-4829',
            },
            params: {
                isYaPlus: true,
                notifications: [yaPlusNotification],
                title: 'Вам 300 баллов за друга',
                text: 'Потратьте их на что-нибудь',
                link: 'https://market.yandex.ru/deals?utm_source=market&utm_medium=banner&utm_campaign=MSCAMP-77utm_term=src_market&utm_content=referal_touch_popup',
            },
        }),
        prepareSuite(LoyaltyNotificationTooltipContentSuite, {
            suiteName:
                'Сообщение о применении промокода, ' +
                'при оформлении заказа, ' +
                'для неплюсовика.',
            meta: {
                id: 'marketfront-4831',
            },
            params: {
                notifications: [nonYaPlusNotification],
                title: 'Вам 300 баллов за друга',
                text: 'Подключите Плюс, чтобы их тратить',
                link: 'https://plus.yandex.ru/?utm_source=market&utm_medium=banner&utm_campaign=MSCAMP-77utm_term=src_market&utm_content=referal_touch_popup',
            },
        })
    ),
});
