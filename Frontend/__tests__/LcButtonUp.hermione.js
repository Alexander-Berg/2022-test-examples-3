specs({
    feature: 'LcButtonUp',
}, () => {
    const optionsAssertView = {
        compositeImage: false,
        allowViewportOverflow: true,
        captureElementFromTop: false
    };

    it('Изначально спрятанная кнопка наверх', function() {
        return this.browser
            .url('/turbo?stub=lcbuttonup/hidden.json')
            .yaWaitForVisible(PO.lcPage(), 'Страница не загрузилась')
            .assertView('hidden', PO.lcPage(), optionsAssertView);
    });

    hermione.skip.in('firefox'); // проверено локально
    it('Круглая кнопка наверх', function() {
        return this.browser
            .url('/turbo?stub=lcbuttonup/circle.json')
            .yaWaitForVisible(PO.lcPage(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .assertView('circle', PO.lcPage(), optionsAssertView);
    });

    it('Закругленная кнопка наверх', function() {
        return this.browser
            .url('/turbo?stub=lcbuttonup/rounded.json')
            .yaWaitForVisible(PO.lcPage(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .assertView('rounded', PO.lcPage(), optionsAssertView);
    });

    it('Квадратная кнопка наверх', function() {
        return this.browser
            .url('/turbo?stub=lcbuttonup/square.json')
            .yaWaitForVisible(PO.lcPage(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .assertView('square', PO.lcPage(), optionsAssertView);
    });
});
