hermione.only.notIn('safari13');

specs({
    feature: 'LcBadgeList',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser.url('/turbo?stub=lcbadgelist/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcBadgeList());
    });

    it('Для iOS виден только стор Apple', function() {
        return this.browser.url('/turbo?stub=lcbadgelist/default_ios.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaShouldBeVisible(PO.lcBadgeList.apple(), 'Стор Apple не показался для iOS')
            .yaShouldNotBeVisible(PO.lcBadgeList.microsoft(), 'Стор Microsoft показался для iOS')
            .yaShouldNotBeVisible(PO.lcBadgeList.google(), 'Стор Google показался для iOS');
    });

    it('Для Android виден только стор Google', function() {
        return this.browser.url('/turbo?stub=lcbadgelist/default_android.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaShouldBeVisible(PO.lcBadgeList.google(), 'Стор Google не показался для Android')
            .yaShouldNotBeVisible(PO.lcBadgeList.apple(), 'Стор Apple показался для Android')
            .yaShouldNotBeVisible(PO.lcBadgeList.microsoft(), 'Стор Microsoft показался для Android');
    });

    it('Для Windows Phone виден только стор Microsoft', function() {
        return this.browser.url('/turbo?stub=lcbadgelist/default_windows_phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaShouldBeVisible(PO.lcBadgeList.microsoft(), 'Стор Microsoft не показался для Windows Phone')
            .yaShouldNotBeVisible(PO.lcBadgeList.apple(), 'Стор Apple показался для Windows Phone')
            .yaShouldNotBeVisible(PO.lcBadgeList.google(), 'Стор Google показался для Windows Phone');
    });
});
