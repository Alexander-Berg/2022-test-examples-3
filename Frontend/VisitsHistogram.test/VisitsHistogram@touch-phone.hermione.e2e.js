const PO = require('./VisitsHistogram.page-object').touchPhone;

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Колдунщик 1орг',
    type: 'Гистограмма посещаемости',
}, function() {
    it('Проверка в гистограммы в сайдблоке', async function() {
        const queries = [
            { text: 'трц европолис спб', lr: 213 },
            { text: 'трц европейский', lr: 213 },
            { text: 'кафе пушкин', lr: 213 },
            { text: 'let\'s rock bar', lr: 213 },
            { text: 'мфц кунцево', lr: 213 },
            { text: 'abc медицина на коломенской', lr: 213 },
            { text: 'третьяковская галерея на якиманке', lr: 213 },
            { text: 'петропавловская крепость', lr: 213 },
            { text: 'sparkle beauty bar на полянке', lr: 213 },
            { text: 'top gun на фрунзенской', lr: 213 },
        ];

        await this.browser.yaSome(queries, async query => {
            await this.browser.yaOpenSerp(query, PO.oneOrg());
            await this.browser.click(PO.oneOrg.tabsMenu.about());
            await this.browser.yaWaitForVisible(PO.overlay.oneOrg(), 3000);
            await this.browser.yaShouldBeVisible(PO.overlay.oneOrg.visitsHistogram());

            return true;
        });
    });
});
