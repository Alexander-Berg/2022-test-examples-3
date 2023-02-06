import {makeCase, makeSuite} from 'ginny';

import LoyaltyNotificationTooltipContent from '@self/root/src/components/LoyaltyNotificationTooltipContent/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {yandexPlusPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

const YANDEX_HELP_COOKIE = 'yandex_help';
const FETCH_LOYALTY_NOTIFICATIONS_TIME_STAMP = 'fetch_loyalty_notifications_time_stamp';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Контент тултипа уведомления от лоялтит.', {
    params: {
        isYaPlus: 'Указывает является ли пользователь плюсовиком',
        notifications: 'Список нотификаций для мока стейта',
        title: 'Заголовок нотификации',
        text: 'Текст нотификации',
        link: 'Url ссылки в нотификации',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                loyaltyNotificationTooltipContent: () => this.createPageObject(LoyaltyNotificationTooltipContent),
            });

            if (this.params.isYaPlus) {
                await this.browser.setState('Loyalty.collections.perks', [yandexPlusPerk]);
            }

            await this.browser.setState('Loyalty.collections.notifications', this.params.notifications);

            // Ставим куку что бы попап онбординга Помощи рядом не открывался и не загораживал попап нотификации
            await this.browser.yaSetCookie({
                name: YANDEX_HELP_COOKIE,
                value: '1',
            });
            await this.browser.yaDeleteCookie(FETCH_LOYALTY_NOTIFICATIONS_TIME_STAMP);

            await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX);

            await this.loyaltyNotificationTooltipContent.waitForVisible();
        },
        'Заголовок содержит корректный текст.': makeCase({
            async test() {
                await this.loyaltyNotificationTooltipContent.getTitle()
                    .should.eventually.to.be.equal(
                        this.params.title,
                        `Текст заголовка, должен содержать ${this.params.title}`
                    );
            },
        }),
        'Информационное сообщение содержит корректный текст.': makeCase({
            async test() {
                await this.loyaltyNotificationTooltipContent.getText()
                    .should.eventually.to.be.equal(
                        this.params.text,
                        `Информационное сообщение должно содержать${this.params.text}`
                    );
            },
        }),
        'Ссылка содержит правильный url.': makeCase({
            async test() {
                await this.loyaltyNotificationTooltipContent.getLinkHref()
                    .should.eventually.be.link(
                        this.params.link
                    );
            },
        }),
    },
});
