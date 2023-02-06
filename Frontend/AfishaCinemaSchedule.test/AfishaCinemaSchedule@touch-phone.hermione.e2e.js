'use strict';

const PO = require('./AfishaCinemaSchedule.page-object')('touch-phone');

hermione.only.notIn('searchapp-phone');
specs({
    feature: '1орг. Витрина фильмов кинотеатра',
}, () => {
    it('Общие проверки', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="companies" and @subtype="company"]/"cinema/buy_tickets"',
            PO.afishaCinema.ticketsButton(),
            [
                '/search/touch/?text=ГУМ Кинозал&lr=213',
                '/search/touch/?text=Формула Кино ЦДМ&lr=213',
                '/search/touch/?text=Формула Кино на Полежаевской&lr=213',
                '/search/touch/?text=Кино Окко Афимолл Сити&lr=213',
            ],
        );
        await this.browser.yaShouldBeVisible(PO.afishaCinema.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.afishaCinema.schedule(), 'Нет расписания');
    });
});
