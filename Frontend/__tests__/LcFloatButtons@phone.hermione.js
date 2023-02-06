specs({
    feature: 'LcFloatButtons',
}, () => {
    var buttonToggleWait = 1000;

    hermione.only.notIn('safari13');
    it('Все виды иконок', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/buttons-1.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part1')
            .url('/turbo?stub=lcfloatbuttons/buttons-2.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part2')
            .url('/turbo?stub=lcfloatbuttons/buttons-3.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part3');
    });

    hermione.only.notIn('safari13');
    it('Оригинальные цвета соцсетей', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/original-buttons-1.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part1')
            .url('/turbo?stub=lcfloatbuttons/original-buttons-2.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part2')
            .url('/turbo?stub=lcfloatbuttons/original-buttons-3.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part3');
    });

    hermione.only.notIn('safari13');
    it('Одна плавающая кнопка', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/single-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    hermione.only.notIn('safari13');
    it('Кнопка слева', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/align-left.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    hermione.only.notIn('safari13');
    it('Две плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/double-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    hermione.only.notIn('safari13');
    it('Показ вне зависимости от скролла', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/show-always.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaScrollPage('#middle', 0)
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaScrollPage('#top', 0)
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaScrollPage('#middle', 0)
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaScrollPage('#bottom', 0)
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait);
    });

    hermione.only.notIn('safari13');
    it('Три плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/triple-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    hermione.only.notIn('safari13');
    it('Четыре плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/quadruple-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    hermione.only.notIn('safari13');
    it('Кнопка чатов', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/yandex-chat-custom-icon.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), 5000)
            .yaAssertViewportView('plain');
    });
});
