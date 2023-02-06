/* eslint-disable no-await-in-loop */

import 'hermione';
import {expect} from 'chai';

import {execScript, login} from '../../helpers';
import {CLEAR_ALL_SEQUENCE, EXTRA_QUOTES_REGEXP} from '../../constants';
import Button from '../../page-objects/button';
import ContentWithLabel from '../../page-objects/contentWithLabel';
import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {BankDetailsFieldNames} from '../../../src/modules/order/components/OrderReturns/constants';

const ORDERS_PAGE_URL = '/entity/root@1?tabBar=6';
const TEST_BIK = '044525974';
const TEST_BANK = 'АО "ТИНЬКОФФ БАНК"';
const TEST_CITY = 'Москва';
const TEST_CORR_ACCOUNT = '30101810145250000974';
const ORDER_STATUS_ID_DELIVERED = 809;
const PAYMENT_TYPE_ID_PREPAID = 7602;

const GET_DELIVERY_STATUS_GID_SCRIPT = `
    api.db.of('orderStatus')
    .withFilters(
      api.db.filters.eq('title', 'доставлен (вручен)'),
    )
    .limit(1)
    .get()
`;

const GET_PAYMENT_TYPE_GID_SCRIPT = `
    api.db.of('orderPaymentType')
    .withFilters(
      api.db.filters.eq('title', 'Предоплата'),
    )
    .limit(1)
    .get()
`;

const getOrderScript = (
    statusId = ORDER_STATUS_ID_DELIVERED,
    paymentTypeId = PAYMENT_TYPE_ID_PREPAID,
    startDT,
    endDT
) => `
    api.db.of('order')
    .withFilters(
      api.db.filters.and(
        api.db.filters.eq('status', ${statusId}),
        api.db.filters.eq('paymentType', ${paymentTypeId}),
        api.db.filters.between('creationDate', '${startDT}', '${endDT}')
      )
    )
    .limit(100)
    .list()
`;

const extractId = gid => gid.replace(EXTRA_QUOTES_REGEXP, '').split('@')[1] || null;
const extractGids = gids =>
    gids
        .replace(EXTRA_QUOTES_REGEXP, '')
        .split(',')
        .filter(gid => gid || false);

const getDates = () => {
    const startDate = new Date();

    startDate.setDate(startDate.getDate() - 14);
    const endDate = new Date();

    endDate.setDate(endDate.getDate() + 7);

    return {start: startDate.toISOString(), end: endDate.toISOString()};
};

describe(`ocrm-1490: На странице создания заказа при вводе БИК автоматически заполняются данные о банке.`, () => {
    beforeEach(function() {
        return login(ORDERS_PAGE_URL, this);
    });

    it('Данные банка подгружаются корректно.', async function() {
        const deliveryStatusGidRaw = await execScript(this.browser, GET_DELIVERY_STATUS_GID_SCRIPT);

        const deliveryStatusId = extractId(deliveryStatusGidRaw);

        expect(deliveryStatusId).to.not.equal(null, 'Не удалось получить ID статуса доставки');

        const paymentTypeGidRaw = await execScript(this.browser, GET_PAYMENT_TYPE_GID_SCRIPT);
        const paymentTypeId = extractId(paymentTypeGidRaw);

        expect(paymentTypeId).to.not.equal(null, 'Не удалось получить ID типа платежа');

        const {start, end} = getDates();
        const orderScript = getOrderScript(deliveryStatusId, paymentTypeId, start, end);

        const orderGidRaw = await execScript(this.browser, orderScript);
        /**
         * В любом заказе уже могут быть выбраны все возможные возвраты, поэтому мы получаем
         * гиды нескольких заказов и пробуем поочередно в котором еще не выбраны все возвраты.
         */
        const orderGids = extractGids(orderGidRaw);

        const isOk = orderGids.every(gid => gid?.includes('@'));

        /**
         * Если тут падает, то это ручка выполнения скриптов по какой-то причине возвращает HTML вместо нормальных данных.
         * При отладке помогали паузы между вызовами в 2-3 секунды.
         */
        expect(isOk).to.equal(
            true,
            'С сервера получены неверные данные при запросе груви скрипта получения гидов заказов'
        );

        let found = false;
        let bank = '';
        let city = '';
        let corrAccount = '';

        for (const gid of orderGids) {
            const url = `/entity/${gid}`;

            await this.browser.url(url);

            const tabsWrapper = new TabsWrapper(
                this.browser,
                'body',
                '[data-ow-test-attribute-container="tabsWrapper"]'
            );

            await tabsWrapper.isDisplayed();
            await tabsWrapper.clickTab('Возвраты');

            const createReturnButton = new Button(this.browser, 'body', '[data-ow-test-return-create]');

            await createReturnButton.isDisplayed();
            /**
             * Пока на странице идет подгрузка заказов и др. информации кнопка создания возврата неактивна, ждем до таймаута,
             * если не активировалась, пропускаем заказ и пробуем следующий.
             */
            await createReturnButton.waitForEnabled();

            const isClickable = await createReturnButton.isEnabled();

            if (isClickable) {
                await createReturnButton.clickButton();

                const bikFld = new ContentWithLabel(
                    this.browser,
                    '[data-ow-test-modal-body]',
                    `[data-ow-test-content-with-label="${BankDetailsFieldNames.BIK}"]`
                );
                const bankFld = new ContentWithLabel(
                    this.browser,
                    '[data-ow-test-modal-body]',
                    `[data-ow-test-content-with-label="${BankDetailsFieldNames.BANK}"]`
                );
                const cityFld = new ContentWithLabel(
                    this.browser,
                    '[data-ow-test-modal-body]',
                    `[data-ow-test-content-with-label="${BankDetailsFieldNames.CITY}"]`
                );
                const corrAccountFld = new ContentWithLabel(
                    this.browser,
                    '[data-ow-test-modal-body]',
                    `[data-ow-test-content-with-label="${BankDetailsFieldNames.CORR_ACCOUNT}"]`
                );

                await bikFld.isDisplayed();
                await bikFld.setValue(CLEAR_ALL_SEQUENCE);
                await bikFld.setValue(TEST_BIK);
                /**
                 * После ввода БИК происходит поход в ручку за реквизитами банка, ждем пару секунд.
                 */
                await bankFld.waitForFilled('Название банка');

                bank = await bankFld.getValue();
                city = await cityFld.getValue();
                corrAccount = await corrAccountFld.getValue();
                found = true;

                break;
            }
        }

        expect(found).to.equal(true, 'Не удалось найти подходящий заказ.');
        expect(bank).to.equal(TEST_BANK, 'Название банка не совпадает.');
        expect(city).to.equal(TEST_CITY, 'Название города не совпадает.');
        expect(corrAccount).to.equal(TEST_CORR_ACCOUNT, 'Корр. счет не совпадает.');
    });
});
