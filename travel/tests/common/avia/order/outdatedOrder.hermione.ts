import moment from 'moment';
import {assert} from 'chai';
import {order} from 'suites/avia';
import {random} from 'lodash';

import {AviaOrderPage} from 'helpers/project/avia/pages';
import * as cities from 'helpers/project/avia/data/cities';

describe(order.name, function () {
    beforeEach(async function () {
        try {
            const date = moment()
                .add(random(-7, -1), 'day')
                .format('YYYY-MM-DD');

            await this.browser.url(
                order.url({
                    from: cities.msk,
                    to: cities.ekb,
                    startDate: date,
                    forward: `SU%201404.${date}T15%3A20`,
                }),
            );
        } catch (e) {
            console.error(order.name, e instanceof Error ? e.message : e);
        }
    });

    it('Непротухающая страница покупки', async function () {
        const page = new AviaOrderPage(this.browser);

        await page.waitForLoading();

        assert(
            (await page.forward.flights.items).length,
            'Отсутствует информация о рейсе',
        );
        assert(
            await page.outdateError.isDisplayed(),
            'Не отображается ошибка о протухшем поиске',
        );
    });
});
