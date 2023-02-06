specs({
    feature: 'LcCarousel',
}, () => {
    hermione.only.notIn('safari13');
    it('Должен отобразить компонент LcCarousel', function() {
        return this.browser
            .url('/turbo?stub=lccarousel/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcCarousel());
    });

    hermione.only.notIn('safari13');
    it('Проверка работоспособности', function() {
        return this.browser
            .url('/turbo?stub=lccarousel/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('start', PO.page())
            .click(PO.blocks.lcCarousel.secondBullet())
            .yaWaitForVisible(PO.blocks.lcCarousel.secondSlide(), 'Второй слайд не виден')
            .assertView('second', PO.page());
    });

    hermione.only.in('chrome-phone', 'touch команды поддерживаются только на реальных устройствах');
    hermione.only.notIn('safari13');
    it('Проверка работоспособности в телефоне', function() {
        return this.browser
            .url('/turbo?stub=lccarousel/default.json')
            .yaWaitForVisible(PO.blocks.lcCarousel(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaTouchScroll(PO.blocks.lcCarousel(), 600)
            .yaWaitForVisible(PO.blocks.lcCarousel.secondSlide(), 'Второй слайд не виден')
            .assertView('second', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Должен отобразить компонент LcCarousel, при отcутствии цвета стрелок', function() {
        return this.browser
            .url('/turbo?stub=lccarousel/withoutArrowColor.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcCarousel());
    });
});
