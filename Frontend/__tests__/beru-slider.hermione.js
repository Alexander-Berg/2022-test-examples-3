specs({
    feature: 'beru-slider',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока по умолчанию', function() {
        return this.browser
            .url('/turbo?stub=beruslider/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Проверка работоспособности', function() {
        return this.browser
            .url('/turbo?stub=beruslider/dynamic.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('start', PO.page())
            .click(PO.blocks.turboCardSliderBeru.secondBullet())
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru.secondSlide(), 'Второй слайд не виден')
            .assertView('second', PO.page());
    });

    hermione.only.in('chrome-phone', 'touch команды поддерживаются только на реальных устройствах');
    hermione.only.notIn('safari13');
    it('Проверка работоспособности в телефоне', function() {
        return this.browser
            .url('/turbo?stub=beruslider/dynamic.json')
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaTouchScroll(PO.blocks.turboCardSliderBeru(), 200)
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru.secondSlide(), 'Второй слайд не виден')
            .assertView('second', PO.page())
            .yaTouchScroll(PO.blocks.turboCardSliderBeru(), 200)
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru.thirdSlide(), 'Третий слайд не виден')
            .assertView('third', PO.page());
    });
});
