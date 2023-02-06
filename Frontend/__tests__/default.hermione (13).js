// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Contests - Controls', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contests%20%7C%20Controls&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
