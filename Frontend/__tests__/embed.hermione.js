const { checkEmbedLoaded, checkEmbedError } = require('./helper.hermione');
const PO = require('../../../../hermione/page-objects');

if (process.env.BUILD_BRANCH && process.env.BUILD_BRANCH.includes('pull')) {
    hermione.skip.in(/.*/, 'не надо тестировать внешние ресурсы');
} else {
    hermione.only.in(['chrome-desktop', 'firefox', 'chrome-phone']); // firefox в списке обязателен из-за различия обработки <object>
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

    describe('default-src', () => {
        hermione.only.notIn('safari13');
        it('Загрузка', function() {
            return this.browser
                .url('/turbo?stub=embed/default-src.json')
                .then(() => checkEmbedLoaded('default-src', this.browser));
        });

        hermione.only.notIn('safari13');
        it('Схлопывается при ошибке', function() {
            return this.browser
                .url('/turbo?stub=embed/default-src-error.json')
                .then(() => checkEmbedError('default-src', this.browser));
        });

        hermione.only.notIn('safari13');
        it('Проверка подписи под эмбедом', function() {
            return this.browser
                .url('/turbo?stub=embed/default-src.json')
                .yaWaitForVisible(PO.page(), 'Страница должна загрузиться ')
                .yaShouldBeVisible(PO.embedCaption(), 'Caption не отрендерился')
                .assertView('caption', PO.embedCaption(), { ignoreElements: [PO.embed.content()] })
                .yaCheckLink({
                    selector: PO.embedCaption.link(),
                    message: 'Неправильная ссылка',
                    target: '_blank',
                    url: {
                        href: 'http://glassmoon.ru/',
                    },
                });
        });
    });
});
