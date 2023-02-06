specs({
    feature: 'LcJobsHeader',
}, () => {
    it('Базовый вид', function() {
        return this.browser
            .url('turbo?stub=lcjobsheader/default.json')
            .yaWaitForVisible(PO.lcJobsHeader(), 'Хедер не появился')
            .assertView('default', PO.lcJobsHeader());
    });

    hermione.only.in(['chrome-desktop', 'firefox']);
    it('На средних экранах', function() {
        return this.browser
            .setViewportSize({ width: 960, height: 900 })
            .pause(300)
            .url('turbo?stub=lcjobsheader/default.json')
            .yaWaitForVisible(PO.lcJobsHeader(), 'Блок не появился')
            .assertView('slim', PO.lcJobsHeader())
            .click(PO.lcJobsHeader.togglerButton())
            .assertView('slim_opened', PO.lcJobsHeader());
    });

    hermione.only.in(['searchapp', 'iphone', 'chrome-phone']);
    it('На узких экранах', function() {
        return this.browser
            .url('turbo?stub=lcjobsheader/default.json')
            .yaWaitForVisible(PO.lcJobsHeader(), 'Блок не появился')
            .click(PO.lcJobsHeader.burger())
            .moveToObject(PO.lcJobsHeader.activeMenuItem())
            .assertView('smallest_opened', PO.lcJobsHeader.window());
    });
});
