const {
    checkEmbedLoaded,
    checkEmbedError,
    checkEmbedSize,
    checkEmbedNonlazy,
    checkEmbedNoSandbox,
} = require('./helper.hermione');

if (process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull')) {
    hermione.skip.in(/.*/, 'не надо тестировать внешние ресурсы');
} else {
    hermione.only.in('chrome-phone', 'Так как тест долгий и у нас нет особой зависимости от устройства есть смысл проверять только на одном дивайсе');
}

specs({
    feature: 'Эмбед',
}, () => {
    afterEach(function() {
        return this.browser
            .pause(1000)
            .yaCheckClientErrors();
    });

    const testCases = [
        // 'clipiki-video',
        'dailymotion-old',
        'dailymotion-video',
        'default-html',
        'default-old',
        'default-src',
        // 'dzenkino-video',
        'facebook-comment',
        'facebook-comments',
        'facebook-like-button',
        'facebook-old',
        'facebook-page',
        'facebook-post',
        'facebook-share-button',
        'facebook-video',
        'instagram-old',
        'instagram-post',
        'instagram-igtv',
        'izlesene-old',
        'izlesene-video',
        'mail-video',
        'mailru-old',
        'mailru-video',
        'myvi-video',
        'ok-group',
        'ok-share',
        'ok-video',
        'rutube-old',
        'rutube-video',
        'twitter-air-button',
        'twitter-follow-button',
        'twitter-moment',
        'twitter-old',
        'twitter-timeline',
        'twitter-tweet',
        'twitter-tweet-button',
        'twitter-video',
        'vimeo-old',
        'vimeo-video',
        'vk-allow-messages-from-community',
        'vk-comments',
        'vk-community-messages',
        'vk-contact-us',
        'vk-like',
        'vk-poll',
        'vk-post',
        'vk-recommended',
        'vk-share',
        'vk-subscribe',
        'vk-video',
        'yamusic-album',
        'yamusic-track',
        'yamusic-playlist',
        'youtube-old',
        'youtube-video',
        'playbuzz-convo',
        'playbuzz-flip-card',
        'playbuzz-list',
        'playbuzz-personality-quiz',
        'playbuzz-poll',
        'playbuzz-quiz',
        'playbuzz-ranked-list',
        'playbuzz-trivia',
        'playbuzz-video',
        'market-models',
        'market-offers',
        'market-specifications',
        'apester-quiz',
    ];

    testCases.forEach(testCase => {
        hermione.only.notIn('safari13');
        it(`Проверка на загрузку эмбеда ${testCase}`, function() {
            return this.browser
                .url(`/turbo?stub=embed/${testCase}.json`)
                .execute(function() {
                    window.document.querySelector('.turbo-embed').scrollIntoView();
                })
                .then(() => checkEmbedLoaded(testCase, this.browser));
        });
    });

    describe('vh-плеер', () => {
        const testCase = 'vh';

        hermione.only.notIn('safari13');
        it('Горизонтальный', function() {
            return this.browser
                .url('/turbo?stub=embed/vh.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 332, height: 187 }, this.browser));
        });

        hermione.only.notIn('safari13');
        it('Горизонтальный с ошибкой эмбеда', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-embed-error.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 332, height: 187 }, this.browser));
        });

        hermione.only.notIn('safari13');
        it('Горизонтальный с ошибкой эмбеда без фолбека', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-embed-error-no-fallback.json')
                .yaWaitForVisible(PO.pageJsInited())
                .yaLoadIframe(PO.ebmedIFrame())
                .then(() => checkEmbedError(testCase, this.browser));
        });

        hermione.only.notIn('safari13');
        it('Горизонтальный с ошибками эмбеда и видео', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-video-error.json')
                .yaWaitForVisible(PO.pageJsInited())
                .yaLoadIframe(PO.ebmedIFrame())
                // Таймаут на схлопывание эмбеда в случае неполучения сообщений - 20 секунд.
                .then(() => checkEmbedError(testCase, this.browser, 21000));
        });

        hermione.only.notIn('safari13');
        it('Вертикальный', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-long.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 332, height: 332 }, this.browser));
        });

        hermione.only.notIn('safari13');
        it('Вертикальный с ошибкой эмбеда', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-long-embed-error.json')
                .then(() => checkEmbedNonlazy(testCase, this.browser))
                .then(() => checkEmbedSize({ width: 332, height: 332 }, this.browser));
        });

        hermione.only.notIn('safari13');
        it('Вставлены без песочницы', function() {
            return this.browser
                .url('/turbo?stub=embed/vh-embed-no-sandbox.json')
                .then(() => checkEmbedNoSandbox(testCase, this.browser));
        });
    });

    describe('market-виджет', () => {
        hermione.only.notIn('safari13');
        it('В src iframe эмбеда указан скрипт из данных', function() {
            return this.browser
                .url('/turbo?stub=embed/market-with-script-url.json')
                .execute(() => document.querySelector('iframe').src)
                .then(result => assert.include(result.value, 'example.com'));
        });

        hermione.only.notIn('safari13');
        it('В src iframe эмбеда указан скрипт из флага', function() {
            return this.browser
                .url('/turbo?stub=embed/market-with-script-url.json&exp_flags=market-widget-url=https://example2.com/')
                .execute(() => document.querySelector('iframe').src)
                .then(result => assert.include(result.value, 'example2.com'));
        });
    });
});
