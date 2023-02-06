'use strict';

const PO = require('./AfishaCinemaSchedule.page-object')('touch-phone');

specs({
    feature: '1орг. Витрина фильмов кинотеатра',
}, () => {
    describe('Сеансы в карточке', function() {
        it('Общие проверки', async function() {
            await this.browser.yaOpenSerp({
                lr: '213',
                text: 'кинотеатр синема парк екатеринбург',
                data_filter: 'companies',
                srcskip: 'YABS_DISTR',
            }, PO.companiesComposite());

            await this.browser.yaTicketDealerScan();
            await this.browser.assertView('plain', PO.afishaCinema());

            await this.browser.yaOpenOverlayAjax(
                () => this.browser.click(PO.companiesComposite.tabsMenu.about()),
                PO.bcardSideBlock(),
                'Сайдблок с карточкой организации не появился',
            );

            await this.browser.yaShouldBeVisible(PO.bcardSideBlock.afisha(), 'Нет блока афиши в оверлее');
        });
    });
});
