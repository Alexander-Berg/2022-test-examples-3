describe('Code-Editor', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Code%20Editor&selectedStory=default')
            .assertView('default', selector);
    });
});
