async function loginAndOpen(browser, PO) {
    await browser.yaLoginReadonly();
    await browser.yaOpenPage('account/show', PO.SettingsShow());
}

const censorSkillsOptions = { ignoreElements: ['.skill-icon'] };

describe('Настройки - шоу Алисы', () => {
    it('Внешний вид', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);
        await browser.yaAssertViewBottomSheet('plain', 'body', censorSkillsOptions);
    });

    it('Раскрытие списков', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);
        const targetSection = PO.SettingsShow.section.nthChild(2);

        await browser.click(targetSection.listItem.toggle());
        await browser.yaAssertView('off', targetSection(), censorSkillsOptions);

        await browser.click(targetSection.listItem.toggle());
        assert((await browser.$$(targetSection.listItem())).length === 4, 'Не совпадает количество строк с ожидаемым в скрытом виде');

        await browser.click(targetSection.expandButton());
        assert((await browser.$$(targetSection.listItem())).length > 4, 'Раскрытие не изменило число строк');
    });

    it('Сохранение источников', async function() {
        const { browser, PO } = this;
        await loginAndOpen(browser, PO);

        const targetSection = PO.SettingsShow.section.nthChild(2);
        const targetListItem = targetSection.listItem.nthChild(2).toggle;
        await browser.click(targetListItem());
        await browser.pause(1000);
        await browser.yaAssertView('toggled', targetSection(), censorSkillsOptions);

        await browser.click(targetListItem());
        await browser.pause(1000);
        await browser.yaAssertView('another-state', targetSection(), censorSkillsOptions);
    });
});
