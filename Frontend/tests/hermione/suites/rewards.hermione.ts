import assert from 'assert';
import { authorize } from '../auth';
import { delay, setup } from '../common';
import { historyRewardsDetailsPath, historyRewardsPath, rewardsPath } from '../common';

describe('Раздел Агентские премии', function() {
    beforeEach(async({ browser }) => {
        setup(browser);
    });

    it('набор сервисов на дашборде совпадает с первой строкой на странице исторических премий', async({ browser }) => {
        await browser.url(rewardsPath);
        await browser.$('.rewards-dashboard__type').waitForDisplayed();

        const servicesOnDashboard = await browser.$$('.rewards-dashboard-item__heading').map(el => el.getText());

        await browser.url(historyRewardsPath);
        await (await browser.$('.table-body')).waitForExist;
        await delay(1000); // TODO remove delay

        const servicesOnHistory = await browser
            .$('.reward-service-icons-group')
            .$$('.reward-service-icons-group__item')
            .map(el => el.getAttribute('data-service'));

        assert(servicesOnDashboard.every(service => servicesOnHistory.includes(service)));
    });

    it('набор сервисов на странице детализации совпадает со строкой на странице исторических премий', async({ browser }) => {
        await authorize(browser);

        await browser.url(historyRewardsPath);
        await browser.$('.table-row').waitForDisplayed();

        const servicesOnHistory = await browser
            .$('.reward-service-icons-group')
            .$$('.reward-service-icons-group__item')
            .map(el => el.getAttribute('data-service'));

        await browser.$('.reward-service-icons-group').click();
        await browser.$('[data-test-id="detailsList"]').waitForDisplayed();

        const servicesOnDetail = await browser
            .$('[data-test-id="detailsList"]')
            .$$('.single-reward-page__service')
            .map(service => service.getText());

        assert(servicesOnHistory.every(service => servicesOnDetail.includes(service)));
    });

    it('страница детализации содержит шапку со статусами, датами и суммами', async({ browser }) => {
        await authorize(browser);

        await browser.url(historyRewardsDetailsPath);
        await browser.$('[data-test-id="period"]').waitForDisplayed();

        assert(browser.$('[data-test-id="period"]').isDisplayed());
        assert(browser.$('[data-test-id="infoColumns"]').isDisplayed());
    });

    it('страница детализации без документов содержит предупреждение', async({ browser }) => {
        await authorize(browser);

        await browser.url(`${historyRewardsPath}/143397`);
        await browser.$('[data-test-id="period"]').waitForDisplayed();

        assert(browser.$('.typography=Данные по премиям за текущие периоды появятся в ближайшее время'));
    });

    it('страница детализации содержит таблицу с доступными для скачивания документами', async({ browser }) => {
        await authorize(browser);

        await browser.url(`${historyRewardsPath}/150887`);

        await browser.$('[data-test-id="documents"]').waitForDisplayed();
        await browser.$('[data-test-id="documents"]').scrollIntoView();
        await browser.$('[data-test-id="documents"]').$('.single-reward-page__actions').click();
        assert(await browser.$('span=Скачать').isDisplayed());
    });
});
