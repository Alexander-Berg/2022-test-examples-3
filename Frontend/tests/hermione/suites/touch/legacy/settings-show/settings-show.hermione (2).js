async function loginAndOpen(browser, PO) {
    await browser
        .authAnyOnRecord('with-devices')
        .yaOpenPage('promo')
        .yaOpenPage('account/show')
        .waitForVisible(PO.SettingsShow(), 10_000);
}

const censorSkillsOptions = { ignoreElements: ['.skill-icon'] };

describe('Настройки - шоу Алисы', () => {
    it('Внешний вид', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);
        await browser.assertView('plain', 'body', censorSkillsOptions);
    });

    it('Раскрытие списков', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);
        const targetSection = PO.SettingsShow.section.nthChild(2);

        await browser.click(targetSection.listItem.toggle());
        await browser.assertView('off', targetSection(), censorSkillsOptions);

        await browser.click(targetSection.listItem.toggle());
        await assert((await browser.$$(targetSection.listItem())).length === 4, 'Не совпадает количество строк с ожидаемым в скрытом виде');

        await browser.click(targetSection.expandButton());
        await assert((await browser.$$(targetSection.listItem())).length > 4, 'Раскрытие не изменило число строк');
    });

    it('Сохранение источников', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);

        const targetSection = PO.SettingsShow.section.nthChild(2);
        const targetListItem = targetSection.listItem.nthChild(2).toggle;
        await browser.click(targetListItem());
        await browser.pause(1000);
        await browser.assertView('toggled', targetSection(), censorSkillsOptions);

        await browser.click(targetListItem());
        await browser.pause(1000);
        await browser.assertView('another-state', targetSection(), censorSkillsOptions);
    });
});
