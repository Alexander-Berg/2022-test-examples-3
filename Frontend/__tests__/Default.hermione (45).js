// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Hint', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Hint&selectedStory=Default')
            .assertView('Default', selector);
    });
});
