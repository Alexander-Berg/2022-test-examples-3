const getPlatformByBrowser = require('../../../../hermione/utils/get-platform-by-browser');

function getHeaderCtaSelectorByPlatform(browser) {
    const platform = getPlatformByBrowser(hermione, browser);

    return platform === 'desktop' ? PO.lcHeaderLpc.callToActionDesktop() : PO.lcHeaderLpc.callToActionMobile();
}

specs({
    feature: 'LcModal',
}, () => {
    let headerCtaSelector;

    beforeEach(function() {
        headerCtaSelector = getHeaderCtaSelectorByPlatform(this.browser);
    });

    hermione.only.notIn('safari13');
    it('Модальное окно с закрепленной шапкой', function() {
        return this.browser
            .url('/turbo?stub=lcmodal/with-fixed-header.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(`.lc-header-lpc__fixed-wrapper ${headerCtaSelector}`)
            // Модальное окно рендерится через портал
            // из-за этого селектор из PO формируется неправильно
            .assertView('plain', ['.lc-modal__overlay']);
    });
});
