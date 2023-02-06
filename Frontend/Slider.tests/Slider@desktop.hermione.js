specs({
    feature: 'Slider',
}, () => {
    it('Проверка работы desktop слайдера', function() {
        let currentTransform;

        return this.browser
            .url('?stub=slider%2Fbasic-5-slides.json')
            .yaWaitForVisible(PO.slider(), 'Слайдер должен быть на месте')
            .yaShouldNotBeVisible(PO.slider.dotsCarousel(), 'Навигационных точек не должно быть')
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Номер активного слайда должен быть 1');
            })
            .yaIndexify(PO.slider.item())
            .getCssProperty(PO.slider.thirdItem.videoThumb(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде не должна быть загружена');
            })
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в четвертом слайде не должна быть загружена');
            })
            .waitUntil(
                () => this.browser.getHTML(PO.blocks.imageSimpleLoaded()),
                5000,
                'Не загрузилась картинки'
            )
            .assertView('arrows-no', PO.slider())
            .moveToObject(PO.slider())
            .yaShouldNotBeVisible(PO.slider.arrows.leftArea(), 'На первом слайде кнопки влево не должно быть')
            .yaShouldBeVisible(PO.slider.arrows.rightArea(), 'На первом слайде кнопка вправо должна быть на месте')
            .assertView('arrows-right-only', PO.slider())
            .yaTouchScroll(PO.slider.content(), 200)
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Свайп не должен работать');
            })
            .click(PO.slider.arrows.rightArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда должен быть 2');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                currentTransform = prop.value;
            })
            .waitUntil(
                () => this.browser.getHTML('.turbo-video-thumb__loader'),
                5000,
                'Не загружается видеотумб'
            )
            .getCssProperty(PO.slider.thirdItem.videoThumb(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде должна быть загружена');
            })
            .click(PO.slider.arrows.rightArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда должен быть 3');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                assert.notEqual(prop.value, currentTransform, 'Третий слайд должен стать видимым');
            })
            .click(PO.slider.arrows.rightArea())
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в четвертом слайде должна быть загружена');
            })
            .click(PO.slider.arrows.leftArea())
            .click(PO.slider.arrows.leftArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Кнопка назад должна работать, номер слайда должен быть 2');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд должен стать видимым');
            })
            .click(PO.slider.arrows.rightArea())
            .click(PO.slider.arrows.rightArea())
            .assertView('arrows-both', PO.slider())
            .click(PO.slider.arrows.rightArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '5', 'Номер активного слайда должен быть 5');
            })
            .yaShouldNotBeVisible(PO.slider.arrows.rightArea(), 'На последнем слайде кнопки вправо не должно быть')
            .yaShouldBeVisible(PO.slider.arrows.leftArea(), 'На последнем слайде кнопка влево должна быть на месте')
            .assertView('arrows-left-only', PO.slider());
    });

    it('Проверка работы desktop слайдера с блоком video-thumb на Реакте', function() {
        let currentTransform;

        return this.browser
            .url('?stub=slider%2Fbasic-5-slides.json&exp_flags=force-react-video-thumb=1')
            .yaWaitForVisible(PO.slider(), 'Слайдер должен быть на месте')
            .yaShouldNotBeVisible(PO.slider.dotsCarousel(), 'Навигационных точек не должно быть')
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Номер активного слайда должен быть 1');
            })
            .yaIndexify(PO.slider.item())
            .getCssProperty(PO.slider.thirdItem.turboVideoThumb(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде не должна быть загружена');
            })
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в четвертом слайде не должна быть загружена');
            })
            .waitUntil(
                () => this.browser.getHTML(PO.blocks.imageSimpleLoaded()),
                5000,
                'Не загрузилась картинки'
            )
            .assertView('arrows-no', PO.slider())
            .moveToObject(PO.slider())
            .yaShouldNotBeVisible(PO.slider.arrows.leftArea(), 'На первом слайде кнопки влево не должно быть')
            .yaShouldBeVisible(PO.slider.arrows.rightArea(), 'На первом слайде кнопка вправо должна быть на месте')
            .assertView('arrows-right-only', PO.slider())
            .yaTouchScroll(PO.slider.content(), 200)
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Свайп не должен работать');
            })
            .click(PO.slider.arrows.rightArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда должен быть 2');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                currentTransform = prop.value;
            })
            .click(PO.slider.arrows.rightArea())
            .waitUntil(
                () => this.browser.getHTML('.turbo-video-thumb__loader'),
                5000,
                'Не загружается видеотумб'
            )
            .getCssProperty(PO.slider.thirdItem.turboVideoThumb(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде должна быть загружена');
            })
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда должен быть 3');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                assert.notEqual(prop.value, currentTransform, 'Третий слайд должен стать видимым');
            })
            .click(PO.slider.arrows.rightArea())
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в четвертом слайде должна быть загружена');
            })
            .click(PO.slider.arrows.leftArea())
            .click(PO.slider.arrows.leftArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Кнопка назад должна работать, номер слайда должен быть 2');
            })
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд должен стать видимым');
            })
            .click(PO.slider.arrows.rightArea())
            .click(PO.slider.arrows.rightArea())
            .assertView('arrows-both', PO.slider())
            .click(PO.slider.arrows.rightArea())
            .getText(PO.slider.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '5', 'Номер активного слайда должен быть 5');
            })
            .yaShouldNotBeVisible(PO.slider.arrows.rightArea(), 'На последнем слайде кнопки вправо не должно быть')
            .yaShouldBeVisible(PO.slider.arrows.leftArea(), 'На последнем слайде кнопка влево должна быть на месте')
            .assertView('arrows-left-only', PO.slider());
    });
});
