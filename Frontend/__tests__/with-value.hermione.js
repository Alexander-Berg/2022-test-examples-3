describe('Code-Editor', function() {
    it('with-value', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Code%20Editor&selectedStory=with-value')
            .assertView('with-value', selector);
    });
});
