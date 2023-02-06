import {expect} from 'chai';

import {execScript, login} from '../../helpers';
import {TIMEOUT_MS} from '../../constants';

const ORDER_GID = 'order@2112T32961157';
const PAGE_URL = `/entity/${ORDER_GID}`;

const ONE_BLOCK_SCRIPT = `def order = api.db.'${ORDER_GID}'
def customer = order.customer
def authRunService = beanFactory.getBean(ru.yandex.market.jmf.security.AuthRunnerService)
api.bcp.edit(customer,['email':order.buyerEmail,'phone':order.buyerPhone])`;

const TWO_BLOCKS_SCRIPT = `def order = api.db.'${ORDER_GID}'
def customer = order.customer
def authRunService = beanFactory.getBean(ru.yandex.market.jmf.security.AuthRunnerService)
api.bcp.edit(customer,['email':order.buyerEmail+'1','phone':order.buyerPhone])`;

const checkBlocks = async (browser, blocksNumber: number, script: string): Promise<void> => {
    await execScript(browser, script);
    await browser.refresh();
    const orderHeader = await browser.$('[data-ow-test-card-header="customOrderHead"]');

    await orderHeader.waitForDisplayed({
        timeout: TIMEOUT_MS,
        timeoutMsg: 'Шапка карточки заказа не подгрузилась за 10 секунд',
    });

    const nameAndLoginElements = await browser.$$('[data-ow-test-attribute-container="nameAndLogin"]');

    expect(nameAndLoginElements.length).to.equal(blocksNumber, 'Неверное количество отображаемых блоков с информацией');
};

/**
 * Проверяем, что после выполнения скрипта выводится только один блок с информацией о покупателе
 * на странице заказа
 */
describe(`ocrm-1404: Вывод одного блока с информацией, если получатель и покупатель совпадают`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Проверяет, что выводится один блок с информацией`, async function() {
        await checkBlocks(this.browser, 1, ONE_BLOCK_SCRIPT);
    });
});

/**
 * Проверяем, что после выполнения скрипта выводятся два блока с информацией о покупателе и получателе
 * на странице заказа
 */
describe(`ocrm-1405: Вывод двух блоков с информацией, если получатель и покупатель отличаются`, () => {
    beforeEach(function() {
        return login(PAGE_URL, this);
    });

    it(`Проверяет, что выводится два блока с информацией`, async function() {
        await checkBlocks(this.browser, 2, TWO_BLOCKS_SCRIPT);
    });
});
