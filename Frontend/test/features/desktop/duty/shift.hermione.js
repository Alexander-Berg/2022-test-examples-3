const PO = require('./PO');
const langs = ['ru', 'en'];

describe('Дежурства/Смена', function() {
    langs.forEach(lang => {
        describe(`Внешний вид (${lang})`, function() {
            it('Не подтверждена', function() {
                return this.browser
                    // открыть страницу настроек дежурств
                    // (/services/duty_test/duty?interval=P1M&date=27052020&lang={ru|en})
                    .openIntranetPage({
                        pathname: '/services/duty_test/duty',
                        query: { interval: 'P1M', date: '27052020', lang: lang },
                    })

                    // ждём, когда загрузится календарь (появится смена в нем) и исчезнет спинер
                    .waitForVisible(PO.dutyCalendarGrid.intervalPending(), 20000)
                    .waitForVisible(PO.dutyCalendarSpin(), 10000, true)

                    .assertView('simple', PO.dutyCalendarGrid.intervalPending(), { captureElementFromTop: false })

                    .moveToObject(PO.dutyCalendarGrid.intervalPending(), 1, 1)

                    .assertView('hovered', PO.dutyCalendarGrid.intervalPending(), { captureElementFromTop: false })
                    .assertView('duty-shift-details-popup', PO.dutyShiftDetails(), {
                        animationDisabled: true,
                        captureElementFromTop: false,
                        redrawElements: ['.Image_loading'],
                    });
            });

            it('Подтверждена', function() {
                return this.browser
                    // открыть страницу настроек дежурств
                    // (/services/duty_test/duty?interval=P1M&date=12102020&lang={ru|en})
                    .openIntranetPage({
                        pathname: '/services/duty_test/duty',
                        query: { interval: 'P1M', date: '12102020', lang: lang },
                    })

                    // ждём, когда загрузится календарь (появится смена в нем) и исчезнет спинер
                    .waitForVisible(PO.dutyCalendarGrid.intervalApproved(), 20000)
                    .waitForVisible(PO.dutyCalendarSpin(), 10000, true)

                    .scroll(PO.dutyCalendarGridSecond())

                    .assertView('simple', PO.dutyCalendarGrid.intervalApproved(), { captureElementFromTop: false })

                    .moveToObject(PO.dutyCalendarGrid.intervalApproved(), 1, 1)

                    .assertView('hovered', PO.dutyCalendarGrid.intervalApproved(), { captureElementFromTop: false })
                    .assertView('duty-shift-details-popup', PO.dutyShiftDetails(), {
                        animationDisabled: true,
                        captureElementFromTop: false,
                        redrawElements: ['.Image_loading'],
                    });
            });

            it('Подтверждена за другим разработчиком', function() {
                return this.browser
                    // открыть страницу настроек дежурств
                    // (/services/duty_test/duty?interval=P1M&date=07022021&lang={ru|en})
                    .openIntranetPage({
                        pathname: '/services/duty_test/duty',
                        query: { interval: 'P1M', date: '07022021', lang: lang },
                    })

                    // ждём, когда загрузится календарь (появится смена в нем) и исчезнет спинер
                    .waitForVisible(PO.dutyCalendarGridThird.intervalApproved(), 20000)
                    .waitForVisible(PO.dutyCalendarSpin(), 10000, true)

                    .scroll(PO.dutyCalendarGridThird())

                    .assertView('simple', PO.dutyCalendarGridThird.intervalApproved(), { captureElementFromTop: false })

                    .moveToObject(PO.dutyCalendarGridThird.intervalApproved(), 1, 1)

                    .assertView('hovered', PO.dutyCalendarGridThird.intervalApproved(), { captureElementFromTop: false })
                    .assertView('duty-shift-details-popup', PO.dutyShiftDetails(), {
                        animationDisabled: true,
                        captureElementFromTop: false,
                        redrawElements: ['.Image_loading'],
                    });
            });

            it('Форма редактирования смены', function() {
                return this.browser
                    // открыть страницу настроек дежурств
                    // (/services/duty_test/duty?interval=P1M&date=27052020&lang={ru|en})
                    .openIntranetPage({
                        pathname: '/services/duty_test/duty',
                        query: { interval: 'P1M', date: '27052020', lang: lang },
                    })
                    .disableAnimations('*')

                    // ждём, когда загрузится календарь (появится смена в нем)
                    .waitForVisible(PO.dutyCalendarGrid.intervalApproved(), 10000)

                    .click(PO.dutyCalendarGrid.intervalPendingShift())
                    .waitForVisible(PO.suggestItem(), 10000)
                    .assertPopupView(PO.dutyShiftEditModal(), 'duty-shift-edit-modal', PO.dutyShiftEditModalContent());
            });
        });
    });
});
