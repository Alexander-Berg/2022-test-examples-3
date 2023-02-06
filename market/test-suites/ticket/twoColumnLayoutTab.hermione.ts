/* eslint-disable no-await-in-loop */
import 'hermione';
import {expect} from 'chai';

import {TabsWrapper} from '../../../src/controls/TabsControl/__pageObject__';
import {login, turnOnExperiment, turnOffExperiment} from '../../helpers';
import {LONG_TIMEOUT_MS} from '../../constants';

const PAGE_URL = '/entity/ticket@80780302';
const EXPERIMENT_CODE = 'SHOW_CUSTOMER_ORIENTED_TICKET_CARD';

/**
 * Проверяем, что:
 * после включения эксперимента 'Показывать карточку обращения с контентом клиента'
 * в тикете появляется двухколоночная вёрстка, в которой отображется таб "Основные свойства';
 * в табе выводятся все необходимые поля.
 */
describe('ocrm-1669: В двухколоночной верстке выводится таб "Основные свойства', () => {
    beforeEach(function() {
        return login('/', this);
    });
    afterEach(function() {
        return turnOffExperiment(this.browser);
    });

    it('Проверяет наличие всех полей в табе "Основные свойства" после включения эксперимента "Показывать карточку обращения с контентом клиента"', async function() {
        await turnOnExperiment(this.browser, EXPERIMENT_CODE);
        await this.browser.url(PAGE_URL);
        const tabBar = new TabsWrapper(this.browser, 'body', '[data-ow-test-attribute-container="tabsWrapper"]');

        await tabBar.isExisting();
        const mainPropsTab = await this.browser.$('button=Основные свойства');

        const mainPropsTabIsExisting = await mainPropsTab.isExisting();

        if (!mainPropsTabIsExisting) {
            await this.browser.refresh();
        }

        await mainPropsTab.waitForDisplayed({
            timeout: LONG_TIMEOUT_MS,
            timeoutMsg: 'Таба "Основные свойства" нет на странице',
        });

        await mainPropsTab.click();
        const service = await this.browser.$('[data-ow-test-properties-list-attribute="service"]');
        const resolution = await this.browser.$('[data-ow-test-properties-list-attribute="resolution"]');
        const clientEmail = await this.browser.$('[data-ow-test-properties-list-attribute="clientEmail"]');
        const clientPhone = await this.browser.$('[data-ow-test-properties-list-attribute="clientPhone"]');
        const clientName = await this.browser.$('[data-ow-test-properties-list-attribute="clientName"]');
        const priority = await this.browser.$('[data-ow-test-properties-list-attribute="priority"]');
        const registrationDate = await this.browser.$('[data-ow-test-properties-list-attribute="registrationDate"]');
        const tags = await this.browser.$('[data-ow-test-properties-list-attribute="tags"]');
        const categories = await this.browser.$('[data-ow-test-properties-list-attribute="categories"]');
        const stTicket = await this.browser.$('[data-ow-test-properties-list-attribute="stTicket"]');
        const order = await this.browser.$('[data-ow-test-properties-list-attribute="order"]');
        const promoCode = await this.browser.$('[data-ow-test-properties-list-attribute="promoCode"]');

        const allMainProps = [
            service,
            resolution,
            clientEmail,
            clientPhone,
            clientName,
            priority,
            registrationDate,
            tags,
            categories,
            stTicket,
            order,
            promoCode,
        ];

        for (const prop of allMainProps) {
            const isPropExisting = await prop.isExisting();

            expect(isPropExisting).to.equal(true, 'Одного из свойств не хватает на странице');
        }
    });
});
