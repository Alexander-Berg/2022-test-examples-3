const PO = require('./PO');

describe('Дежурства', function() {
    describe('Календарь дежурств', function() {
        it('С пересечением праздничных дней и замены', function() {
            return this.browser
                // открыть календарь дежурств (/services/duty_test/duty/)
                .openIntranetPage({
                    pathname: '/services/duty_test/duty/',
                    query: { date: '07072020', interval: 'P1M' },
                })

                // ждём, когда календарь загрузится
                .waitForVisible(PO.dutyCalendarGrid.interval.holiday(), 20000)

                .assertView('duty-calendar-full', PO.dutyCalendar(), { allowViewportOverflow: true, hideElements: ['.YndxBug', '.tools-lamp'] })
                .assertView('complex-duty-shift', PO.dutyCalendarGrid.intervalApproved(), { captureElementFromTop: false });
        });

        it('Создающийся график', function() {
            return this.browser
                // открыть календарь дежурств (/services/duty_test/duty/)
                .openIntranetPage({
                    pathname: '/services/duty_test/duty/',
                    query: { date: '01062021', interval: 'P1M', role: 5089 },
                })

                // ждём, когда календарь загрузится
                .waitForVisible(PO.dutyCalendarGrid.creatingSchedule.refreshPageBtn(), 10000)

                .assertView('duty-calendar-creating', PO.dutyCalendar(), { allowViewportOverflow: true, hideElements: ['.YndxBug', '.tools-lamp'] });
        });
    });
});
