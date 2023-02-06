const defaultCheckPageConf = { skipProtocol: true, skipHostname: true, skipQuery: true };

describe('Внутренняя страница 404', () => {
    describe('Страница 404', () => {
        beforeEach(function() {
            return this.browser
                .yaOpenPage('/ege/')
                .yaCheckPageUrl('/tutor/ege/', defaultCheckPageConf)
                .yaOpenPage('/ege/?exam_id=4')
                .yaCheckPageUrl('/tutor/ege/?exam_id=4', defaultCheckPageConf, ['exam_id']);
        });

        it('Переход на главную страницу (ЕГЭ)', function() {
            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.HeadTabs.LinkItemFirst()))
                .yaCheckPageUrl('/tutor/ege/', defaultCheckPageConf)
                .getText(PO.HeadTabs.LinkItemActive())
                .then(text => assert(text.trim() === 'ЕГЭ', 'Выбранный таб не является табом ЕГЭ'))
                .yaCheckPageUrl('/tutor/ege/', defaultCheckPageConf, ['exam_id']);
        });

        it('Переход на главную страницу (ОГЭ)', function() {
            return this.browser
                .yaWaitChangeUrl(() => this.browser.click(PO.HeadTabs.LinkItemSecond()))
                .yaCheckPageUrl('/tutor/oge/', defaultCheckPageConf)
                .getText(PO.HeadTabs.LinkItemActive())
                .then(text => assert(text.trim() === 'ОГЭ', 'Выбранный таб не является табом ОГЭ'))
                .yaCheckPageUrl('/tutor/oge/', defaultCheckPageConf, ['exam_id']);
        });
    });
});
