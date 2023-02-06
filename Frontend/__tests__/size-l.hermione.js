describe('Create-Button', function() {
    it('size-l', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Create%20Button&selectedStory=size-l')
            .assertView('size-l', selector);
    });
});
