specs({
    feature: 'Footer React',
}, () => {
    describe('Внешний вид', function() {
        hermione.only.notIn('safari13');
        it('default', function() {
            return this.browser
                .url('/turbo?stub=footer/default.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('view-inline', function() {
            return this.browser
                .url('/turbo?stub=footer/view-inline.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('default-copyright', function() {
            return this.browser
                .url('/turbo?stub=footer/default-copyright.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('with-agreement', function() {
            return this.browser
                .url('/turbo?stub=footer/with-agreement.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('with-copyrights-link', function() {
            return this.browser
                .url('/turbo?stub=footer/with-copyrights-link.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('without-copyrights', function() {
            return this.browser
                .url('/turbo?stub=footer/without-copyrights.json')
                .yaWaitForVisible(PO.footer())
                .assertView('plain', PO.footer());
        });
    });

    hermione.only.notIn('safari13');
    it('Проверка ссылок внутри футера', function() {
        return this.browser
            .url('/turbo?stub=footer/default.json')
            .yaWaitForVisible(PO.footer())
            .yaCheckLink({
                selector: PO.footer.links.fullVersion(),
                url: {
                    href: 'https://ya.ru',
                    ignore: ['pathname', 'query'],
                },
                message: 'Сломана ссылка "Полная версия"',
            })
            .yaCheckBaobabCounter(PO.footer.links.fullVersion(), {
                path: '$page.$main.$result.footer.link-full',
            })
            .yaCheckLink({
                selector: PO.footer.links.support(),
                url: {
                    href: 'https://yandex.ru/support/abuse/troubleshooting/turbo/list.html',
                    ignore: ['query'],
                },
                message: 'Сломана ссылка "Пожаловаться"',
            })
            .yaCheckBaobabCounter(PO.footer.links.support(), {
                path: '$page.$main.$result.footer.link-support',
            });
    });

    hermione.only.notIn('safari13');
    it('Проверка открытия модального окна', function() {
        return this.browser
            .url('/turbo?stub=footer/with-agreement.json')
            .yaWaitForVisible(PO.footer())
            .click(PO.footer.more())
            .yaWaitForVisible(PO.footerModal(), 'Модальное окно не показалось')
            .assertView('modal-agreement', PO.turboModal())
            .click(PO.footerModal.close())
            .yaWaitForHidden(PO.footerModal(), 'Модальное окно не скрылось');
    });

    hermione.only.notIn('safari13');
    it('Проверка открытия модального окна c длинным текстом', function() {
        return this.browser
            .url('/turbo?stub=footer/with-agreement-long-text.json')
            .yaWaitForVisible(PO.footer())
            .click(PO.footer.more())
            .yaWaitForVisible(PO.footerModal(), 'Модальное окно не показалось')
            .assertView('modal-agreement', PO.blocks.footerModalWrapper())
            .yaScrollElement(PO.blocks.footerModalWrapper(), 0, 9999)
            .assertView('modal-agreement-scrolled', PO.turboModal())
            .click(PO.footerModal.close())
            .yaWaitForHidden(PO.footerModal(), 'Модальное окно не скрылось');
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Проверка открытия модального окна в ландшафтной ориентации', function() {
        return this.browser
            .setOrientation('landscape')
            .url('/turbo?stub=footer/with-agreement.json')
            .yaWaitForVisible(PO.footer())
            .click(PO.footer.more())
            .yaWaitForVisible(PO.footerModal(), 'Модальное окно не показалось')
            .assertView('modal-agreement', PO.turboModal())
            .click(PO.blocks.footerModalLandscapeClose())
            .yaWaitForHidden(PO.footerModal(), 'Модальное окно не скрылось');
    });

    hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Не падает, если предоставлен неправильный урл', function() {
        return this.browser
            .url('/turbo?stub=footer/invalid-url.json')
            .yaWaitForVisible(PO.footer())
            .getAttribute(PO.footer.links.fullVersion(), 'href')
            .then(value => assert.equal(value, 'https://tv.yandex.ru/'));
    });

    describe('Проверка локализации', function() {
        hermione.only.notIn('safari13');
        it('Английский', function() {
            return this.browser
                .url('/turbo?stub=footer/default.json&l10n=en')
                .yaWaitForVisible(PO.footer())
                .assertView('localization-en', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('Турецкий', function() {
            return this.browser
                .url('/turbo?stub=footer/default.json&l10n=tr')
                .yaWaitForVisible(PO.footer())
                .assertView('localization-tr', PO.footer());
        });

        hermione.only.notIn('safari13');
        it('Английский для википедии', function() {
            return this.browser
                .url('/turbo?stub=footer/for-wiki.json&l10n=en')
                .yaWaitForVisible(PO.footer())
                .assertView('localization-wiki-en', PO.page());
        });

        hermione.only.notIn('safari13');
        it('Для отсутствующей локализации показываем русский', function() {
            return this.browser
                .url('/turbo?stub=footer/for-wiki.json&l10n=zz')
                .yaWaitForVisible(PO.footer())
                .assertView('localization-wiki-zz', PO.page());
        });
    });
});
