// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('Side Block', function() {
    it('big-content', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Side%20Block&selectedStory=big-content')
            .assertView('big-content', selector);
    });
});
