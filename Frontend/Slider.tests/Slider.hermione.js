describe('Slider', () => {
    hermione.skip.in(['win-ie11']);
    it('horizontal', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .assertView('plain', ['#horizontal']);
    });

    hermione.skip.in(['win-ie11']);
    it('outlined', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .assertView('plain', ['#outlined']);
    });

    hermione.skip.in(['win-ie11']);
    it('overriden', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .assertView('plain', ['#overriden']);
    });

    hermione.skip.in(['win-ie11']);
    it('visual-behavior', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .assertView('plain', ['#horizontal-sipmple .Slider'])
            .moveToObject('#horizontal-sipmple .Slider-Container')
            .assertView('container-hovered', ['#horizontal-sipmple .Slider'])
            .moveToObject('#horizontal-sipmple .Slider-Thumb')
            .assertView('thumb-hovered', ['#horizontal-sipmple .Slider'])
            .buttonDown()
            .assertView('thumb-pressed', ['#horizontal-sipmple .Slider']);
    });

    hermione.skip.in(['win-ie11']);
    it('visual-disabled-behavior', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .assertView('plain', ['#horizontal-disabled .Slider'])
            .moveToObject('#horizontal-disabled .Slider-Container')
            .assertView('container-hovered', ['#horizontal-disabled .Slider'])
            .moveToObject('#horizontal-disabled .Slider-Thumb')
            .assertView('thumb-hovered', ['#horizontal-disabled .Slider'])
            .buttonDown()
            .assertView('thumb-pressed', ['#horizontal-disabled .Slider']);
    });

    hermione.skip.in(['win-ie11']);
    it('functional-behavior', function() {
        return this.browser
            .url('Slider/hermione/hermione.html')
            .moveToObject('#horizontal-sipmple .Slider-Thumb')
            .buttonDown()
            .moveToObject('#horizontal-sipmple .Slider-Track', 100, 0)
            .buttonUp()
            .assertView('move-update', ['#horizontal-sipmple .Slider'])
            .moveToObject('#horizontal-sipmple .Slider-Track', 0, 0)
            .buttonDown()
            .assertView('click-update', ['#horizontal-sipmple .Slider']);
    });
});
