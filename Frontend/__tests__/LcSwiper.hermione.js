specs({
    feature: 'LcSwiper',
}, () => {
    hermione.only.notIn('safari13');
    it('Обертка над swiper c несколькими слайдами', function() {
        return this.browser
            .url('/turbo?stub=lcswiper/default.json')
            .yaWaitForVisible(PO.lcSwiper(), 'Секция LcSwiper не появилась')
            .assertView('plain', PO.lcSwiper());
    });

    hermione.only.notIn('safari13');
    it('Обертка над swiper c одним слайдом', function() {
        return this.browser
            .url('/turbo?stub=lcswiper/one.json')
            .yaWaitForVisible(PO.lcSwiper(), 'Секция LcSwiper не появилась')
            .assertView('plain', PO.lcSwiper());
    });
});
