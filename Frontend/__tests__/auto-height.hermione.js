describe('Code-Editor', function() {
    it('auto-height', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Code%20Editor&selectedStory=auto-height')
            .assertView('auto-height', selector);
    });
});
