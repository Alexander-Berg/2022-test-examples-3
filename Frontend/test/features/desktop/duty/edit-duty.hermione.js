const PO = require('./PO');
const langs = ['ru', 'en'];

describe('Дежурства', function() {
    langs.forEach(lang => {
        describe(`Форма редактирования дежурства (${lang})`, function() {
            it('Внешний вид', function() {
                return this.browser
                    // открыть страницу настроек дежурств
                    // (/services/duty_test/duty?interval=P1M&date=27052020&lang={ru|en})
                    .openIntranetPage({
                        pathname: '/services/duty_test/duty',
                        query: { interval: 'P1M', date: '27052020', lang: lang },
                    })
                    .disableAnimations('*')

                    // ждём, когда загрузится календарь (появится смена в нем) и исчезнет спинер
                    .waitForVisible(PO.dutyCalendarGrid.intervalApproved(), 10000)
                    .waitForVisible(PO.dutyCalendarSpin(), 10000, true)

                    .click(PO.dutyCalendarChangeSchedule())

                    .waitForVisible(PO.dutyScheduleEditModal())
                    // предотвращение ховера на строке в форме
                    .moveToObject(PO.dutyScheduleEditModal(), 0, 0)
                    // единственная видимая модалка - нужная
                    .assertPopupView(PO.visibleModal(), 'duty-shift-edit-modal', PO.dutyScheduleEditModal());
            });
        });
    });
});
