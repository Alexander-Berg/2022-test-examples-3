hermione.only.notIn('linux-chrome-ipad', 'нет такой функциональности');
describe('Страница варианта', () => {
    it('Карточка справочных материалов', function() {
        return this.browser
            .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16')
            .yaScroll(PO.TimerCard())
            .assertView('promo', PO.PromoReference());
    });

    describe('Сайдблок', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16')
                .yaFixStickyOnPage();
        });

        it('Проверка открытия/закрытия сайдблока', function() {
            return this.browser
                .click(PO.PromoReference.Btn())
                .yaWaitForVisible(PO.OpenSideBlock())
                .assertView('side-block-open', PO.OpenSideBlock(), { allowViewportOverflow: true })
                .click(PO.SideBlock.Header.closeButton())
                .yaWaitForHidden(PO.OpenSideBlock());
        });

        it('Переход на теорию', function() {
            return this.browser
                .yaOpenPage('/subject/variant/?variant_id=1')
                .yaFixStickyOnPage()
                .click(PO.PromoReference.Btn())
                .yaWaitForVisible(PO.OpenSideBlock())
                .yaCheckLink(
                    PO.SideBlock.Header.TheoryLink(),
                    '/tutor/subject/theory/?subject_id=1',
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    },
                    ['subject_id']
                );
        });
    });

    describe('Таймер', function() {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16');
        });

        it('Нет решенных задач', function() {
            return this.browser
                .getText(PO.TimerClock.TestInfo.FirstRow())
                .then(text => {
                    assert(text.trim() === 'Выполнено заданий: 0 из 20');
                })
                .assertView('clock', PO.TimerCard(), {
                    allowViewportOverflow: true,
                    ignoreElements: [PO.TimerClock.Timer(), PO.TimerClock.Description()],
                });
        });
        it('Есть решенные задачи', function() {
            return this.browser
                .setValue(PO.Task.input(), '111')
                .getText(PO.TimerClock.TestInfo.FirstRow())
                .then(text => assert(text.trim() === 'Выполнено заданий: 20 из 20'));
        });

        it('Пауза', function() {
            let timerValue;

            return this.browser
                .pause(1000)
                .click(PO.TimerClock.Switcher())
                .assertView('suspend-clock', PO.TimerCard(), {
                    allowViewportOverflow: true,
                    ignoreElements: [PO.TimerClock.Timer(), PO.TimerClock.Description()],
                })
                .getText(PO.TimerClock.Timer())
                .then(text => {
                    timerValue = text;
                    assert(text < '03:00:00', 'Значение таймера должно быть меньше 3х часов');
                })
                .getText(PO.TimerClock.Description())
                .then(pastTime => {
                    // исходим из предположения что тест не может длиться больше минуты
                    // поэтому складываем прошедшие секунды
                    assert(Number(pastTime.slice(-2)) + Number(timerValue.slice(-2)) === 60, 'суммарно время обоих счетчиков секунд равно 60');
                });
        });

        it('Оверлей', function() {
            return this.browser
                .click(PO.TimerClock.Switcher())
                .yaWaitForVisible(PO.VariantTest.BlurContent())
                .click(PO.Pause.Button())
                .yaWaitForHidden(PO.VariantTest.BlurContent())
                .setValue(PO.Task.input(), '111')
                .click(PO.TimerClock.Switcher())
                .getText(PO.Pause.TaskCount())
                .then(text => assert(text.trim() === 'Выполнено заданий: 20 из 20'));
        });
    });

    describe('Модальное окно', function() {
        describe('При попытке уйти со страницы варианта', function() {
            it('В том же окне', function() {
                const homeUrl = '/tutor/';
                const animationDelay = 200;
                const expectedText = 'Вы уверены, что хотите уйти со страницы?';
                const skipUrlOptions = { skipProtocol: true, skipHostname: true, skipQuery: true };

                return this.browser
                    .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16')
                    .click(PO.Header.LogoLinkTutor())
                    .pause(animationDelay)
                    .yaWaitForVisible(PO.LeaveModal())
                    .assertView('leave-modal', PO.LeaveModal.Content())
                    .getText(PO.LeaveModal.Text()).then(actualText => assert.equal(actualText, expectedText))
                    .click(PO.LeaveModal.Decline())
                    .pause(animationDelay)
                    .click(PO.Header.LogoLinkTutor())
                    .pause(animationDelay)
                    .yaWaitForVisible(PO.LeaveModal())
                    .yaWaitChangeUrl(() => this.browser.click(PO.LeaveModal.Accept()))
                    .yaCheckPageUrl(homeUrl, skipUrlOptions);
            });

            it('В новом окне', function() {
                return this.browser
                    .yaOpenPage('/subject/variant/?variant_id=1&subject_id=16')
                    .yaScroll(PO.TimerCard()) // Иначе TimerCard перекрывает иконку репорта на карточке задания
                    .click(PO.Task.ReportLink())
                    .yaWaitForHidden(PO.LeaveModal());
            });
        });
    });
});
