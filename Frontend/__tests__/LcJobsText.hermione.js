specs({
    feature: 'LcJobsText',
}, () => {
    [
        'header-xss',
        'header-xs',
        'header-s',
        'header-m',
        'header-l',
        'header-xl',
        'header-xxl',
        'text-s',
        'text-m',
        'text-l',
        'with-link'
    ].forEach(item => {
        hermione.only.notIn('safari13');
        it(item, function() {
            return this.browser
                .url(`turbo?stub=lcjobstext/${item}.json`)
                .yaWaitForVisible(PO.lcJobsText(), 'Текст не появился')
                .assertView('default', PO.lcJobsText());
        });
    });
});
