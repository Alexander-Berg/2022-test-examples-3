// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Side Block', function() {
    it('small', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Side%20Block&selectedStory=small')
            .assertView('small', selector);
    });
});
