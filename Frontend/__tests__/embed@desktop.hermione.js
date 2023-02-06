const { checkEmbedError, checkEmbedSize, checkEmbedNonlazy } = require('./helper.hermione');

if (process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull')) {
    hermione.skip.in(/.*/, 'не надо тестировать внешние ресурсы');
} else {
    hermione.only.in(['chrome-desktop', 'firefox']); // firefox в списке обязателен из-за различия обработки <object>
    hermione.skip.in('firefox'); // в гриде в firefox нет поддержки видео
}

specs({
    feature: 'Эмбед',
}, () => {
    afterEach(function() {
        return this.browser
            .pause(1000)
            .yaCheckClientErrors();
    });

    describe('vh-плеер', () => {
        const testCase = 'vh';

        it('Горизонтальный', function() {
            return this.browser
                .url('/turbo?stub=embed/vh.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 728, height: 410 }, this.browser));
        });

        it('Горизонтальный с ошибкой эмбеда', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-embed-error.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 728, height: 410 }, this.browser));
        });

        it('Горизонтальный с ошибкой эмбеда без фолбека', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-embed-error-no-fallback.json')
                .yaWaitForVisible(PO.pageJsInited())
                .yaLoadIframe(PO.ebmedIFrame())
                .then(() => checkEmbedError(testCase, this.browser));
        });

        it('Горизонтальный с ошибками эмбеда и видео', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-video-error.json')
                .yaWaitForVisible(PO.pageJsInited())
                .yaLoadIframe(PO.ebmedIFrame())
                // Таймаут на схлопывание эмбеда в случае неполучения сообщений - 20 секунд.
                .then(() => checkEmbedError(testCase, this.browser, 21000));
        });

        it('Вертикальный', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-long.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 728, height: 546 }, this.browser));
        });

        it('Вертикальный с ошибкой эмбеда', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-long-embed-error.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 728, height: 546 }, this.browser));
        });
    });
});
