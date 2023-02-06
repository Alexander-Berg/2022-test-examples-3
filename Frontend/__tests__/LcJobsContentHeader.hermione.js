specs({
    feature: 'LcJobsContentHeader',
}, () => {
    const custom_timeout = 60000;

    hermione.only.notIn(['safari13']);
    it('default', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/default.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('default', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('size_s', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/size_s.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('size_s', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('size_l', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/size_l.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('size_l', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_button', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_button.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .yaWaitForVisible(PO.lcJobsContentHeader.Button(), custom_timeout, 'Кнопка в контент-шапке не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_button', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_tabs', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_tabs.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .yaWaitForVisible(PO.lcJobsContentHeader.RadioButton(), custom_timeout, 'Табы в контент-шапке не появились')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_tabs', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_share', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_share.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .yaWaitForVisible(PO.lcShare(), custom_timeout, 'Блок \'поделиться\' не появился')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_share', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_logo', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_logo.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_logo', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_tabs_and_button', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_tabs_and_button.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .yaWaitForVisible(PO.lcJobsContentHeader.Button(), custom_timeout, 'Кнопка в контент-шапке не появилась')
            .yaWaitForVisible(PO.lcJobsContentHeader.RadioButton(), custom_timeout, 'Табы в контент-шапке не появились')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_tabs_and_button', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_tabs_and_share', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_tabs_and_share.json')
            .moveToObject(PO.lcJobsContentHeader())
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .yaWaitForVisible(PO.lcShare(), custom_timeout, 'Блок поделиться не появился')
            .yaWaitForVisible(PO.lcJobsContentHeader.RadioButton(), custom_timeout, 'Табы в контент-шапке не появились')
            .assertView('with_tabs_and_share', PO.lcJobsContentHeader());
    });

    hermione.only.notIn('safari13');
    it('with_gradient', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_gradient.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_gradient', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('disabled', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/disabled.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('disabled', PO.lcJobsContentHeader());
    });

    hermione.only.notIn(['safari13']);
    it('with_button_only', function() {
        return this.browser
            .url('turbo?stub=lcjobscontentheader/with_button_only.json')
            .yaWaitForVisible(PO.lcJobsContentHeader(), custom_timeout, 'Контент-шапка вакансий не появилась')
            .moveToObject(PO.lcJobsContentHeader())
            .assertView('with_button_only', PO.lcJobsContentHeader());
    });
});
