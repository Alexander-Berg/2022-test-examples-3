specs({
    feature: 'LcIcon',
}, () => {
    hermione.only.notIn('safari13');
    it('Все виды иконок', function() {
        return this.browser
            .url('/turbo?stub=lcicon/default.json')
            .yaWaitForVisible(PO.lcIcons(), 'Страница не загрузилась')
            .assertView('plain', PO.lcIcons());
    });

    hermione.only.notIn('safari13');
    it('Стилизация иконки', function() {
        return this.browser
            .url('/turbo?stub=lcicon/with-custom-style.json')
            .yaWaitForVisible(PO.lcIcons(), 'Страница не загрузилась')
            .assertView('plain', PO.lcIcons());
    });

    hermione.only.notIn('safari13');
    it('Размеры иконок', function() {
        return this.browser
            .url('/turbo?stub=lcicon/with-size.json')
            .yaWaitForVisible(PO.lcIcons(), 'Страница не загрузилась')
            .assertView('plain', PO.lcIcons());
    });

    hermione.only.notIn('safari13');
    it('Свойство hidden', function() {
        return this.browser
            .url('/turbo?stub=lcicon/hidden.json')
            .yaWaitForVisible(PO.lcIcons(), 'Страница не загрузилась')
            .assertView('plain', PO.lcIcons());
    });

    hermione.only.notIn('safari13');
    it('Иконка со ссылкой', function() {
        return this.browser
            .url('/turbo?stub=lcicon/link.json')
            .yaWaitForVisible(PO.lcIcon(), 'Страница не загрузилась')
            .assertView('plain', PO.lcIcon())
            .moveToObject(PO.lcIcon())
            .assertView('hovered', PO.lcIcon())
            .yaCheckLink({
                selector: PO.lcIcon(),
                message: 'Неправильная ссылка',
                target: '',
                url: {
                    href: 'https://yandex.ru/turbo?stub=title/default.json',
                },
            });
    });
});
