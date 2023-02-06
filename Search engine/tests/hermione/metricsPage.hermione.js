describe('MetricsPage', function () {
    it('should open list', async function () {
        await this.browser.url('/admin/all-metrics');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('MetricsPage-list');
    });

    it('should open list with params', async function () {
        await this.browser.url('/admin/all-metrics?type=judged&text=geo');
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('MetricsPage-list-with-params');
    });

    it('should open list with owner and deprecated', async function () {
        await this.browser.url(
            '/admin/all-metrics?owner=nata-tischenko&deprecated=true',
        );
        await this.browser.waitForFetching();
        await this.browser.assertBodyView('MetricsPage-list-with-params-2');
    });

    it('should open creation form', async function () {
        await this.browser.url('/admin/all-metrics/new');
        await this.browser.assertBodyView('MetricsPage-new', {
            ignoreElements: ['[data-testid=metrics-responsible-users]'],
        });
    });

    it('should open metric', async function () {
        await this.browser.url('/admin/all-metrics/24video_top_1_part');
        await this.browser.assertBodyView('MetricsPage-metric', {
            ignoreElements: ['[data-testid=metrics-responsible-users]'],
        });
    });

    it('should open clone form', async function () {
        await this.browser.url('/admin/all-metrics/24video_top_1_part/clone');
        await this.browser.assertBodyView('MetricsPage-clone', {
            ignoreElements: ['[data-testid=metrics-responsible-users]'],
        });
    });

    it('should open metric - show aspects click', async function () {
        await this.browser.url('/admin/all-metrics/empty-serp');

        await this.browser.waitForExist('[data-test-id=MetricAspects]');
        await this.browser.$('[data-test-id=MetricAspects]').click();
        await this.browser.assertBodyView('MetricsPage-metric-show-aspects', {
            ignoreElements: ['[data-testid=metrics-responsible-users]'],
        });

        await this.browser.$('[data-test-id=MetricSpecifications]').click();
        await this.browser.assertBodyView(
            'MetricsPage-metric-show-specifications',
            {ignoreElements: ['[data-testid=metrics-responsible-users]']},
        );
    });
});
