describe('Viewer', function() {
    it('должен открыться по клику на изображение', function() {
        return this.browser
            .yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 292,
                    pcgi: 'rnd=fnu7e56jbff',
                },
            })
            .yaWaitForVisibleWithinViewport('.Slider-Slide')
            .click('.Slider-Slide')
            .yaWaitForVisibleWithinViewport(PO.blocks.viewer())
            .yaShouldBeVisible(PO.blocks.viewer(), 'Просмотрщик не открылся')
            .assertView('plain', PO.blocks.viewer());
    });

    it('должен закрыться по клику на крестик', function() {
        return this.browser
            .yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 292,
                    pcgi: 'rnd=fnu7e56jbff',
                },
            })
            .yaWaitForVisibleWithinViewport('.Slider-Slide')
            .click('.Slider-Slide')
            .yaWaitForVisibleWithinViewport(PO.blocks.viewer())
            .yaShouldBeVisible(PO.blocks.viewer(), 'Просмотрщик не открылся')
            .yaWaitForVisibleWithinViewport(PO.blocks.viewerHeader.close())
            .click(PO.blocks.viewerHeader.close())
            .yaShouldNotBeVisible(PO.blocks.viewer(), 'Просмотрщик не закрылся');
    });

    it('должна работать браузерная навигация', function() {
        return this.browser
            .yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 292,
                    pcgi: 'rnd=fnu7e56jbff',
                },
            })
            .yaWaitForVisibleWithinViewport('.Slider-Slide')
            .click('.Slider-Slide')
            .yaWaitForVisibleWithinViewport(PO.blocks.viewer())
            .yaShouldBeVisible(PO.blocks.viewer(), 'Просмотрщик не открылся')
            .back()
            .yaShouldNotBeVisible(PO.blocks.viewer(), 'Просмотрщик не закрылся')
            .forward()
            .yaShouldBeVisible(PO.blocks.viewer(), 'Просмотрщик не открылся');
    });

    it('перелистывание картинок в просмотрщике', function() {
        return this.browser
            .yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 292,
                    pcgi: 'rnd=fnu7e56jbff',
                },
            })
            .yaWaitForVisibleWithinViewport('.Slider-Slide')
            .click('.Slider-Slide')
            .yaWaitForVisibleWithinViewport(PO.blocks.viewer())
            .yaShouldBeVisible(PO.blocks.viewer(), 'Просмотрщик не открылся')
            .getCssProperty(PO.viewerSliderScroll(), 'transform')
            .then(prop => assert.equal(prop.value, 'none'))
            .yaTouchScroll(PO.viewerSliderScroll(), 300)
            .getCssProperty(PO.viewerSliderScroll(), 'transform')
            .then(prop => assert.notEqual(prop.value, 'none', 'Слайдер не двигался'));
    });
});
