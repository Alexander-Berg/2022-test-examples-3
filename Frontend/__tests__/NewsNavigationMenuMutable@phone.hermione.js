specs({
    feature: 'Панель навигации (изменяемая)',
}, () => {
    const selector = '.turbo-navigation-menu';

    hermione.only.notIn('safari13');
    it('Блок изменяется на проскралливании страницы', function() {
        return this.browser
            .url('/turbo?stub=newsnavigationmenumutable/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .assertView('plain', selector)
            .yaScrollPageToBottom()
            .assertView('mutated', '.turbo-native-scroll__inner');
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Блок нормально выглядит и в горизонтальной ориентации тоже', function() {
        return this.browser
            .url('/turbo?stub=newsnavigationmenumutable/default.json')
            .yaWaitForVisible(selector, 'Блок не появился')
            .setOrientation('landscape')
            .assertView('landscape', selector);
    });
});
