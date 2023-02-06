describe('Contest-Settings---Packages-List', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Generate-New-Package', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Packages%20List&selectedStory=Generate-New-Package',
            )
            .assertView('Generate-New-Package', selector, {
                ignoreElements: '.Spin2',
            });
    });
});
