describe('Заглушка для старых браузеров', function() {
    hermione.only.notIn(['linux-chrome', 'linux-firefox', 'win-edge', 'linux-chrome-ipad'], 'только в старом хроме');
    hermione.also.in('old-chrome');
    it('Страница открывается', function() {
        return this.browser
            .yaOpenPage('/ege/')
            .assertView('plain', 'body');
    });
});
