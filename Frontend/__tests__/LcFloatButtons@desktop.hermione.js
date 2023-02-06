specs({
    feature: 'LcFloatButtons',
}, () => {
    var buttonToggleWait = 1000;

    it('Все виды иконок', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/buttons-1.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part1')
            .moveToObject(PO.lcFloatButtons.button() + '[data-idx="0"]')
            .yaAssertViewportView('hovered')
            .url('/turbo?stub=lcfloatbuttons/buttons-2.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part2')
            .url('/turbo?stub=lcfloatbuttons/buttons-3.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('part3');
    });

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

    it('Одна плавающая кнопка', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/single-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain')
            .moveToObject(PO.lcFloatButtons())
            .yaAssertViewportView('hovered');
    });

    it('Кнопка слева', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/align-left.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    it('Две плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/double-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    it('Три плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/triple-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    it('Четыре плавающие кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/quadruple-button.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    it('Кнопка чатов', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/yandex-chat-custom-icon.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.lcFloatButtons(), buttonToggleWait)
            .yaAssertViewportView('plain');
    });

    it('Кнопка с LcPhone', function() {
        return this.browser
            .url('/turbo?stub=lcfloatbuttons/with-lc-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForVisible(PO.LcLayoutManagerDesktop(), buttonToggleWait)
            .click(PO.LcLayoutManagerDesktop())
            .yaWaitForVisible(PO.lcPhoneModal(), 'Модал не открылся');
    });
});
