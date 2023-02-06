import 'hermione';
import {expect} from 'chai';

import {login} from '../../helpers';
import {TIMEOUT_MS} from '../../constants';

const BLUE_ORDER_LINK = '/entity/order@2105T32452535';
const WHITE_ORDER_LINK = '/entity/order@2009T7212530';

describe('ocrm-852: отображение типа маркета в карточке заказа - Покупки', () => {
    beforeEach(function() {
        return login(BLUE_ORDER_LINK, this);
    });

    it('тип маркета "Покупки" отображается', async function() {
        const orderHeader = await this.browser.$('[data-ow-test-card-header="customOrderHead"]');

        await orderHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Шапка карточки заказа не подгрузилась за 10 секунд',
        });

        const colorType = await this.browser.$$(
            '//span[@data-ow-test-card-header="customOrderHead"]//span[text()="Покупки"]'
        );

        expect(colorType.length).to.equal(1, 'Тип маркета "Покупки" не отображается');
    });
});

describe('ocrm-856: отображение типа маркета в карточке заказа - Белый DSBS', () => {
    beforeEach(function() {
        return login(WHITE_ORDER_LINK, this);
    });

    it('тип маркета "Покупки" отображается', async function() {
        const orderHeader = await this.browser.$('[data-ow-test-card-header="customOrderHead"]');

        await orderHeader.waitForDisplayed({
            timeout: TIMEOUT_MS,
            timeoutMsg: 'Шапка карточки заказа не подгрузилась за 10 секунд',
        });

        const colorType = await this.browser.$$(
            '//span[@data-ow-test-card-header="customOrderHead"]//span[text()="Белый DSBS"]'
        );

        expect(colorType.length).to.equal(1, 'Тип маркета "Белый DSBS" не отображается');
    });
});
