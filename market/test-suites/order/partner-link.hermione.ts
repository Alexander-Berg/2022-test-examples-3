import 'hermione';
import chai from 'chai';

import {PartnerLink} from '../../../src/modules/order/components/PartnerLink/__pageObject__';
import {login} from '../../helpers';

const ORDER_WITH_YANDEX_DELIVERY = '/entity/order@2105T32452535';
const EXPECTED_LINK_TO_PARTNER = 'https://partner.market.yandex.ru/tpl/21638137/orders/32452535';

const ORDER_WITHOUT_YANDEX_DELIVERY = '/entity/order@2105T32504973';

describe('ocrm-1448: Ссылка на Партнёрский интерфейс', () => {
    describe('в заказе с доставкой Яндекса', () => {
        beforeEach(function() {
            return login(ORDER_WITH_YANDEX_DELIVERY, this);
        });

        it('должна отображаться на странице и содержать ссылку на партнёрский интерфейс', async function() {
            const partnerLink = new PartnerLink(this.browser);

            const isDisplayed = await partnerLink.isDisplayed();

            chai.expect(isDisplayed).to.equal(true, 'Ссылка на ПИ видна');

            const link = await partnerLink.getUrl();

            return chai.expect(link).to.equal(EXPECTED_LINK_TO_PARTNER, 'Ссылка на ПИ корректная');
        });
    });

    describe('в заказе без доставки Яндекса', () => {
        beforeEach(function() {
            return login(ORDER_WITHOUT_YANDEX_DELIVERY, this);
        });

        it('не должна отображаться на странице', async function() {
            const partnerLink = new PartnerLink(this.browser);

            const isNotDisplayed = await partnerLink.waitForInvisible();

            return chai.expect(isNotDisplayed).to.equal(true, 'Ссылка на ПИ не видна');
        });
    });
});
