specs({
    feature: 'EcomFooter',
}, () => {
    describe('Внешний вид', function() {
        hermione.only.notIn('safari13');
        it('С данными', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/default.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('Без данных', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/empty.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('Только с контактами', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/only-contacts.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('Только с названием и аккордеоном', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/only-name-with-accordion.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('Только с названием и ссылками', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/only-name-with-links.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('Только с названием', function() {
            return this.browser
                .url('/turbo?stub=ecomfooter/only-name.json')
                .yaWaitForVisible(PO.ecomFooter(), 'Футер не появился на странице')
                .assertView('plain', PO.ecomFooter());
        });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Проверка общих ссылок', function() {
        return this.browser
            .url('/turbo?stub=ecomfooter/default.json')
            .yaWaitForVisible(PO.ecomFooter())
            .yaCheckLink({
                selector: PO.ecomFooter.market(),
                url: {
                    href: 'https://market.yandex.ru',
                    ignore: ['pathname', 'query'],
                },
                message: 'Сломана ссылка на Яндекс.Маркет',
            })
            .yaCheckBaobabCounter(PO.ecomFooter.market(), {
                path: '$page.$main.$result.ecom-footer.link-market',
            })
            .yaCheckLink({
                selector: PO.ecomFooter.links.fullVersion(),
                url: {
                    href: 'https://yandex.ru',
                    ignore: ['pathname', 'query'],
                },
                message: 'Сломана ссылка "Полная версия"',
            })
            .yaCheckBaobabCounter(PO.ecomFooter.links.fullVersion(), {
                path: '$page.$main.$result.ecom-footer.link-full',
            })
            .yaCheckLink({
                selector: PO.ecomFooter.links.support(),
                url: {
                    href: 'https://yandex.ru/support/abuse/troubleshooting/turbo/list.html',
                    ignore: ['query'],
                },
                message: 'Сломана ссылка "Пожаловаться"',
            })
            .yaCheckBaobabCounter(PO.ecomFooter.links.support(), {
                path: '$page.$main.$result.ecom-footer.link-support',
            });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Проверка ссылок контактов', function() {
        return this.browser
            .url('/turbo?stub=ecomfooter/only-contacts.json')
            .yaWaitForVisible(PO.ecomFooter())
            .yaCheckLink({
                selector: PO.ecomFooter.contacts.phoneContact(),
                target: '_parent',
                url: {
                    href: 'tel:88001231212',
                },
                message: 'Сломана ссылка на телефон',
            })
            .yaCheckBaobabCounter(PO.ecomFooter.contacts.phoneContact(), {
                path: '$page.$main.$result.ecom-footer.link-contact',
            })
            .yaCheckLink({
                selector: PO.ecomFooter.contacts.mailContact(),
                target: '_parent',
                url: {
                    href: 'mailto:help@qwe.ru',
                },
                message: 'Сломана ссылка на email',
            })
            .yaCheckBaobabCounter(PO.ecomFooter.contacts.mailContact(), {
                path: '$page.$main.$result.ecom-footer.link-contact',
            });
    });
});
