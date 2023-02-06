import 'hermione';

import {login} from '../../helpers';
import {CHECK_INTERVAL, TIMEOUT_MS} from '../../constants';

describe(`ocrm-562: Перенаправление на новую карточку заказа`, () => {
    beforeEach(function() {
        return login(``, this);
    });

    it(`Редирект на новую карточку происходит успешно`, async function() {
        await this.browser.url(`order/6353324`);

        const {baseUrl = ''} = this.browser.options;

        await this.browser.waitUntil(
            async () => (await this.browser.getUrl()) === `${baseUrl}/entity/order@2006T6353324`,
            {
                timeout: TIMEOUT_MS,
                timeoutMsg: 'Не дождались редиректа на новую карточку заказа',
                interval: CHECK_INTERVAL,
            }
        );
    });
});
