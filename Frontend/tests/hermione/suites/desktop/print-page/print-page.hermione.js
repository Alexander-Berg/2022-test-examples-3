const TASKS = {
    basic: 'T1507',
    answer: 'T311',
    picture: 'T811',
    table: 'T12',
    tabs: 'T4819',
    code: 'T787',
    longText: 'T381',
    shorText: 'T398',
    lists: 'T388',
    orderedList: 'T72',
    list: 'T608',
};
const ANIMATION_DELAY = 500;
const getTaskUrl = taskId => `/subject/problem/?problem_id=${taskId}`;

const getVariantUrl = variantId => `/subject/variant/?variant_id=${variantId}`;

const getPrintUrl = url => `${url}&print=1`;

hermione.only.notIn('linux-chrome-ipad', 'нет такой функциональности');
describe('Страница печати', () => {
    describe('Печать страницы варианта', function() {
        beforeEach(function() {
            return this.browser.yaOpenPage(`${getVariantUrl(326)}&print=1`)
                .yaWaitForVisible(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .pause(ANIMATION_DELAY)
                .click(PO.PrintPdfToolbarControlMenu.CopyPage());
        });

        it('Проверка внешнего вида заголовка', function() {
            return this.browser
                .assertView('print-page-header', PO.Header())
                .assertView('print-page-breadcrumbs', PO.Navigation());
        });

        it('Проверка внешнего вида контролов печати', function() {
            return this.browser.assertView('print-page-controls', PO.PrintControls());
        });

        it('Проверка отсутствия дублирующихся цитат', function() {
            return this.browser
                .assertView('first', PO.PrintContent.FirstTask())
                .assertView('second', PO.PrintContent.SecondTask());
        });

        it('Решения', function() {
            return this.browser
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Solutions())
                .getText(PO.PrintContent.Title())
                .then(text =>
                    assert.equal(text.includes('Решения'), true, 'Неверный конетент таба')
                )
                .assertView('answer-task', PO.PrintContent());
        });

        it('Задания', function() {
            return this.browser
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Tasks())
                .assertView('answer-task', PO.PrintContent());
        });

        it('Ответы', function() {
            return this.browser
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Answers())
                .getText(PO.PrintContent.Title())
                .then(text =>
                    assert.equal(text.includes('Ответы'), true, 'Неверный контент таба')
                )
                .assertView('answer-task', PO.PrintContent());
        });
    });

    describe('Печать страницы топика', function() {
        it('Проверка внешнего вида заголовка', function() {
            return this.browser
                .yaOpenPage('/subject/tag/problems/?ege_number_id=361&print=1&tag_id=19')
                .yaWaitForVisible(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .pause(ANIMATION_DELAY)
                .click(PO.PrintPdfToolbarControlMenu.CopyPage())
                .pause(ANIMATION_DELAY)
                .assertView('print-tag-page', PO.PrintControls());
        });
    });

    describe('Печать страницы задачи', function() {
        function selectCopyView(task) {
            return this.browser
                .yaOpenPage(getPrintUrl(getTaskUrl(task)))
                .yaWaitForVisible(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .pause(ANIMATION_DELAY)
                .click(PO.PrintPdfToolbarControlMenu.CopyPage())
                .pause(ANIMATION_DELAY);
        }

        it('Задача с картинкой', function() {
            return selectCopyView.call(this, TASKS.picture)
                .assertView('picture-task', PO.PrintContent());
        });

        it('Задача с таблицей', function() {
            return selectCopyView.call(this, TASKS.table)
                .assertView('table-task', PO.PrintContent());
        });

        it('Задача с блоком кода с табуляцией', function() {
            return selectCopyView.call(this, TASKS.tabs)
                .assertView('tabs-task', PO.PrintContent());
        });

        it('Задача с блоком кода', function() {
            return selectCopyView.call(this, TASKS.code)
                .assertView('code-task', PO.PrintContent());
        });

        it('Задача с блоком текста - >500 символов', function() {
            return selectCopyView.call(this, TASKS.longText)
                .assertView('long-text-task', PO.PrintContent());
        });

        it('Задача с блоком текста - <500 символов', function() {
            return selectCopyView.call(this, TASKS.shorText)
                .assertView('short-text-task', PO.PrintContent());
        });

        it('Задача со списками', function() {
            return selectCopyView.call(this, TASKS.lists)
                .assertView('lists-task', PO.PrintContent());
        });

        it('Задача с нумерованым списком', function() {
            return selectCopyView.call(this, TASKS.orderedList)
                .assertView('ordered-list-task', PO.PrintContent());
        });

        it('Задача со списком', function() {
            return selectCopyView.call(this, TASKS.list)
                .assertView('list-task', PO.PrintContent());
        });

        it('Печать "Ответы"', function() {
            return selectCopyView.call(this, TASKS.answer)
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Answers())
                .yaWaitForHidden(PO.PrintContent.Answers())
                .assertView('answer-task', PO.PrintContent());
        });

        it('Печать "Решения"', function() {
            return selectCopyView.call(this, TASKS.longText)
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Solutions())
                .getText(PO.PrintContent.Title())
                .then(text =>
                    assert.equal(text.includes('Решения'), true, 'Неверный конетент таба')
                )
                .assertView('answer-task', PO.PrintContent());
        });

        it('Печать "Справочные материалы"', function() {
            return selectCopyView.call(this, TASKS.basic)
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Materials())
                .getText(PO.TheoryContent.Title())
                .then(text =>
                    assert.equal(text, 'Справочные материалы к профильному уровню ЕГЭ по математике', 'Неверный конетент таба')
                )
                .assertView('answer-task', PO.TheoryContent());
        });

        it('Печать "Задания"', function() {
            return selectCopyView.call(this, TASKS.basic)
                .click(PO.PrintControls.SelectPdfFilter())
                .click(PO.SelectPdfFilterControlMenu.Tasks())
                .getText(PO.PrintContent.Title())
                .then(text =>
                    assert.equal(text.includes('Задание'), true, 'Неверный конетент таба')
                )
                .assertView('answer-task', PO.PrintContent());
        });
    });

    describe('Загрузка pdf', function() {
        it('Генерация вертикальной разметки', function() {
            const expectedUrl = 'https://yandex.ru';

            return this.browser
                .yaOpenPage(`${getVariantUrl(1)}&print=1`)
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbarControlMenu.Portrait())
                .yaWaitChangeUrl(() => this.browser.click(PO.PrintPdfToolbar.Download()))
                .yaCheckPageUrl(
                    expectedUrl,
                    { skipQuery: true },
                );
        });

        it('Генерация горизонтальной разметки', function() {
            const expectedUrl = 'https://yandex.ru';

            return this.browser
                .yaOpenPage(`${getVariantUrl(1)}&print=1`)
                .click(PO.PrintPdfToolbar.OrientationSelect())
                .click(PO.PrintPdfToolbarControlMenu.Landscape())
                .yaWaitChangeUrl(() => this.browser.click(PO.PrintPdfToolbar.Download()))
                .yaCheckPageUrl(expectedUrl, { skipQuery: true });
        });
    });
});
