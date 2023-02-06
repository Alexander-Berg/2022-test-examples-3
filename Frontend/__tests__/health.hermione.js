specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    hermione.only.notIn('safari13');
    it('Брендирование', function() {
        return this.browser
            .url('?text=https%3A%2F%2Fhealth.yandex.ru%2Fdiseases%2Finfec%2Fgripp&brand=health')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .assertView('plain', '.yandex-header');
    });

    hermione.only.notIn('safari13');
    it('Статья', function() {
        return this.browser
            .url('?stub=health%2Farticle.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.skip.in(['iphone'], 'на iphone постоянно появляются несколько скроллов.');
    hermione.only.notIn('safari13');
    it('Поиск', function() {
        return this.browser
            .url('?stub=health%2Fsearch.json&brand=health&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('search', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Профиль врача', function() {
        return this.browser
            .url('?stub=health%2Fprofile.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Профиль клиники', function() {
        return this.browser
            .url('?stub=health%2Fprofile-clinic.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Профиль сайта', function() {
        return this.browser
            .url('?stub=health%2Fprofile-website.json&brand=health&exp_flags=adv-disabled=0&hermione_advert=stub&hermione_no-lazy=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .assertView('plain', PO.page());
    });
});
