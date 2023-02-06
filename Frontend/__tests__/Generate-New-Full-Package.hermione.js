describe('Problems---Packages-List', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Generate-New-Full-Package', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Packages%20List&selectedStory=Generate-New-Full-Package',
            )
            .assertView('Generate-New-Full-Package', selector, {
                ignoreElements: '.problem-packages__download-link',
                screenshotDelay: 3000,
            });
    });
});
