import assert from 'assert';
import { authorize } from '../auth';

describe('Раздел "Об агенстве"', function() {
    beforeEach(async({ browser }) => {
        await authorize(browser);

        await browser.yaOpenPageByUrl('/partner-office/666');
        await browser.yaWaitForPageLoad();

        await browser.$('.agency-info__column_area_a').waitForDisplayed({ timeout: 7000 });
        await browser.yaWaitForPageLoad();

        await browser.yaWaitForVisible('[href^="tel:"]');
        //await browser.yaWaitForVisible('.agency-info__column_area_a', 7000);
        //await browser.yaWaitForVisible('.agency-info__column_area_b', 7000);
    });

    it('Номер телефона', async({ browser }) => {
        const tel = await browser.$('[href^="tel:"]').getText();
        assert.strictEqual(tel, '+0 (000) 000-00-00', 'Tel number missing or incorrect:' + tel + ' vs ' + '+0 (000) 000-00-00');
    });

    it('Почта', async({ browser }) => {
        const mail = await browser.$('[href^="mailto:"], [href*="@"]').getText();
        assert.strictEqual(mail, 'ъ@ъъ.ъъъ', 'Email missing or incorrect:' + mail + ' vs ' + 'ъ@ъъ.ъъъ');
    });

    it('Сайт', async({ browser }) => {
        const site = await browser.$('[href^="https"]').getText();
        assert.strictEqual(site, 'www.asd.com', 'Site missing or incorrect:' + site + ' vs ' + 'www.asd.com');
    });

    it('Фактический адрес', async({ browser }) => {
        const address1 = await browser.$('.agency-info__column_area_a > div:nth-child(4) > div:nth-child(2)').getText();
        assert.strictEqual(address1, 'actual_address', 'Actual address missing or incorrect:' + address1 + ' vs ' + 'actual_address');
    });

    it('Юридический адрес', async({ browser }) => {
        const address2 = await browser.$('.agency-info__column_area_a > div:nth-child(5) > div:nth-child(2)').getText();
        assert.strictEqual(address2, 'legal_address', 'Legal address missing or incorrect:' + address2 + ' vs ' + 'legal_address');
    });

    it('ID агенства', async({ browser }) => {
        const id = await browser.$('.agency-info__column_area_b > div:nth-child(1) > div:nth-child(2)').getText();
        assert.strictEqual(id, '666', 'ID missing or incorrect:' + id + ' vs ' + '666');
    });

    it('Номер контракта', async({ browser }) => {
        const contract_number = await browser.$('.agency-info__column_area_b > div:nth-child(2) > div:nth-child(2) > div:nth-child(1)').getText();
        assert.strictEqual(contract_number, '123/456', 'Conract number missing or incorrect:' + contract_number + ' vs ' + '123/456');
    });

    it('ИНН контракта', async({ browser }) => {
        const contract_inn = await browser.$('.agency-info__column_area_b > div:nth-child(2) > div:nth-child(2) > div:nth-child(2)').getText();
        assert.strictEqual(contract_inn, 'ИНН 666', 'INN missing or incorrect:' + contract_inn + ' vs ' + 'ИНН 666');
    });

    it('Дата контракта', async({ browser }) => {
        const contract_date = await browser.$('.agency-info__column_area_b > div:nth-child(2) > div:nth-child(2) > div:nth-child(3)').getText();
        assert.strictEqual(contract_date, 'до 1 января 2030', 'Conract date missing or incorrect:' + contract_date + ' vs ' + 'до 1 января 2030');
    });
});
