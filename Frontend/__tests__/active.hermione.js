describe('Create-Button', function() {
    it('active', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Create%20Button&selectedStory=active')
            .assertView('active', selector);
    });
});
