const getPlatformByBrowser = require('../../../../hermione/utils/get-platform-by-browser');

const moveCursorCommands = {
    desktop: 'moveToObject',
    'touch-phone': 'touch',
};

const clickCommands = {
    desktop: 'click',
    'touch-phone': 'touch',
};

const arrowNextPOs = {
    desktop: PO.lcFeaturesDesktopCarouselArrowNext(),
    'touch-phone': PO.lcFeaturesMobileCarouselArrowNext(),
};

const firstDotPOs = {
    desktop: PO.lcFeaturesDesktopCarouselFirstDot(),
    'touch-phone': PO.lcFeaturesMobileCarouselFirstDot(),
};

let moveCursorCommand;
let clickCommand;

specs({
    feature: 'LcFeatures',
}, () => {
    beforeEach(function() {
        moveCursorCommand = getValueByPlatform(this.browser, moveCursorCommands);
        clickCommand = getValueByPlatform(this.browser, clickCommands);
    });

    hermione.only.notIn('safari13');
    it('Стандартные фичи', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/default.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('default', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с кнопками', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/cta-buttons.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('cta-buttons', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с кнопками и дисклеймерами', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/cta-buttons-disclaimers.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('cta-buttons-disclaimers', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи со ссылками', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/cta-links.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('cta-links', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с подложкой', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/cover.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('cover', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с кнопками, которые появляются на ховер', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/cta-on-hover.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('cta-on-hover', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [moveCursorCommand](PO.lcFeaturesFirstItem())
            .assertView('cta-on-hover_hovered', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с обратной стороной (эффект переворот)', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/flip.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('flip', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [moveCursorCommand](PO.lcFeaturesFirstItem())
            .assertView('flip_hovered', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с обратной стороной (эффект переворот) и подложкой', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/flip-with-substrate.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('flip-with-substrate', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [moveCursorCommand](PO.lcFeaturesFirstItem())
            .assertView('flip-with-substrate_hovered', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с обратной стороной (эффект слайда)', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/slide.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('slide', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [moveCursorCommand](PO.lcFeaturesFirstItem())
            .assertView('slide_hovered', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с выравниванием по центру', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/default-align-center.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('default-align-center', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с выравниванием по правому краю', function() {
        return this.browser
            .url('/turbo?stub=lcfeatures/default-align-right.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('default-align-right', PO.lcFeatures());
    });

    hermione.only.notIn('safari13');
    it('Фичи с каруселью', function() {
        const arrowNextPO = getValueByPlatform(this.browser, arrowNextPOs);
        const firstDotPO = getValueByPlatform(this.browser, firstDotPOs);

        return this.browser
            .url('/turbo?stub=lcfeatures/carousel.json')
            .yaScrollPageToBottom()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('carousel', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [clickCommand](arrowNextPO)
            .assertView('carousel_swiped', PO.lcFeatures())
            // eslint-disable-next-line no-unexpected-multiline
            [clickCommand](firstDotPO)
            .assertView('carousel_returned', PO.lcFeatures());
    });
});

function getValueByPlatform(browser, commands) {
    const platform = getPlatformByBrowser(hermione, browser);

    return commands[platform];
}
