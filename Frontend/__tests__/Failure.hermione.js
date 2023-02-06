// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Bubble', function() {
    it('Failure', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Bubble&selectedStory=Failure')
            .assertView('Failure', selector);
    });
});
