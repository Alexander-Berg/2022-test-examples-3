const subjectUrl = '/subject/?subject_id=2';

describe('Страница предмета', () => {
    hermione.only.notIn('linux-chrome-ipad', 'нет ховера на падах');
    describe('Подсказка о варианте', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/subject/?subject_id=2')
                .moveToObject(PO.VariantButtonExtended.Cover())
                .waitForVisible(PO.VariantTipPopup());
        });

        it('Проверка внешнего вида', function() {
            return this.browser.assertView('plain', PO.VariantTipPopup());
        });

        it('Проверка перехода на страницу печати варианта', function() {
            const printLink = '/tutor/subject/variant/?subject_id=2&variant_id=311&print=1';

            return this.browser
                .yaCheckTargetBlank(PO.VariantTipPrintLink())
                .yaCheckLink(
                    PO.VariantTipPrintLink(),
                    printLink,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    },
                    ['subject_id', 'print'],
                );
        });
    });

    describe('Карточка вариантов', function() {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/ege/')
                .yaWaitChangeUrl(() => this.browser.click(PO.SubjectItemSecond()))
                .getText(PO.BreadCrumbs.last.ItemText())
                .then(text =>
                    assert.equal(text, 'Математика (профильный уровень)', 'Oткрыта неверная страница предмета')
                );
        });

        hermione.only.notIn('linux-chrome-ipad', 'на падах по клику появляется пупап');
        it('Переход на страницу варианта', function() {
            const link = 'https://local.yandex.ru:3443/tutor/subject/variant/?subject_id=2&variant_id=367';

            return this.browser
                .yaWaitForVisible(PO.TopicListCard())
                .assertView('plain', PO.TopicListCard())
                .yaWaitChangeUrl(() => this.browser.click(PO.EduCardVariant.VariantButtonFirst()))
                .yaCheckPageUrl(
                    link,
                    { skipProtocol: true, skipHostname: true },
                    ['variant_id', 'subject_id']
                )
            ;
        });

        hermione.only.in('linux-chrome-ipad', 'нет ховера на падах');
        it('Переход на страницу варианта ipad', function() {
            const link = 'https://local.yandex.ru:3443/tutor/subject/variant/?subject_id=2&variant_id=367';

            return this.browser
                .yaWaitForVisible(PO.TopicListCard())
                .assertView('plain', PO.TopicListCard())
                .click(PO.EduCardVariant.VariantButtonFirst())
                .yaShouldBeVisible(PO.VariantTipPopup())
                .yaWaitChangeUrl(() => this.browser.click(PO.VariantTipButton()))
                .yaCheckPageUrl(
                    link,
                    { skipProtocol: true, skipHostname: true },
                    ['variant_id', 'subject_id']
                )
            ;
        });
    });

    describe('Карточка задания дня', function() {
        it('Проверка внешнего вида и ссылки в кнопке', function() {
            const link = 'https://yandex.ru/tutor/subject/problem/?problem_id=T34';

            return this.browser
                .yaOpenPage(subjectUrl)
                .assertView('plain', PO.PromoTaskOfTheDay())
                .getAttribute(
                    PO.PromoTaskOfTheDay.Controls.Btn(),
                    'target'
                )
                .then(text =>
                    assert.equal(text, '_self', 'Не откроется в новой вкладке')
                )
                .yaCheckLink(
                    PO.PromoTaskOfTheDay.Controls.Btn(),
                    link,
                    { skipProtocol: true, skipHostname: true },
                );
        });
    });

    describe('Карточка самого сложного задания', function() {
        it('Проверка внешнего вида и ссылки в кнопке', function() {
            const link = 'https://yandex.ru/tutor/subject/problem/?problem_id=T1256';

            return this.browser
                .yaOpenPage(subjectUrl)
                .assertView('plain', PO.PromoHardestTask())
                .getAttribute(
                    PO.PromoHardestTask.Controls.Btn(),
                    'target'
                )
                .then(text =>
                    assert.equal(text, '_self', 'Не откроется в новой вкладке')
                )
                .yaCheckLink(
                    PO.PromoHardestTask.Controls.Btn(),
                    link,
                    { skipProtocol: true, skipHostname: true },
                );
        });
    });
});
