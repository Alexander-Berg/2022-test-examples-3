specs({
    feature: 'EcomPromoBadge',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=ecompromobadge/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.ecomPromoBadge())
            .yaCheckLink({
                selector: PO.ecomPromoBadge.iconLink(),
                url: {
                    href: 'https://yandex.ru/dev/turbo/?utm_source=turbo-badge',
                },
                message: 'Сломана ссылка на информацию о Турбо',
            })
            .yaCheckBaobabCounter(PO.ecomPromoBadge.iconLink(), {
                path: '$page.$main.$result.ecom-promo-badge.link',
            })
            .yaCheckLink({
                selector: PO.ecomPromoBadge.contentLink(),
                url: {
                    href: 'https://yandex.ru/dev/turbo/?utm_source=turbo-badge',
                },
                message: 'Сломана ссылка на информацию о Турбо',
            })
            .yaCheckBaobabCounter(PO.ecomPromoBadge.contentLink(), {
                path: '$page.$main.$result.ecom-promo-badge.link',
            });
    });
});
