specs({
    feature: 'LcText',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lctext/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcText());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с дополнительными свойствами', function() {
        return this.browser
            .url('/turbo?stub=lctext/extended.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcText());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с выравниванием по центру и ограниченной шириной', function() {
        return this.browser
            .url('/turbo?stub=lctext/center.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcText());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с выравниванием по правой стороне и ограниченной шириной', function() {
        return this.browser
            .url('/turbo?stub=lctext/right.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcText());
    });

    hermione.only.notIn('safari13');
    it('Отсутствие габаритов при отсутствии текста', function() {
        return this.browser
            .url('/turbo?stub=lctext/empty.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .execute(function(lcTextSelector) {
                return document.querySelector(lcTextSelector).offsetHeight;
            }, PO.lcText())
            .then(({ value }) => {
                assert.equal(value, 0, 'Пустой текст имеет габариты');
            });
    });
});
