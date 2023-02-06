'use strict';

const PO = require('./AfishaCinemaSchedule.page-object')('desktop');

specs({
    feature: '1орг. Витрина фильмов кинотеатра (десктоп)',
}, () => {
    it('Проверка наличия элементов', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]//afisha_cinema/title',
            PO.oneOrg.afishaCinema(),
            [
                '/search/?text=мега теплый стан расписание сеансов&lr=213',
                '/search/?text=атлас кинотеатр новосибирск&lr=213',
                '/search/?text=планета кинотеатр красноярск&lr=213',
            ],
        );
        await this.browser.yaShouldBeVisible(PO.oneOrg.afishaCinema.title.link(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.oneOrg.afishaCinema.showcase(), 'Нет шоукейса');
    });
});
