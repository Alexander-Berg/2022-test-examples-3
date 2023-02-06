// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Bubble', function() {
    it('With-link', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Bubble&selectedStory=With-link')
            .assertView('With-link', selector);
    });
});
