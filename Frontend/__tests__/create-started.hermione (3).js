describe('Problemsets---Create-Modal', function() {
    it('create-started', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemsets%20%7C%20Create%20Modal&selectedStory=create-started',
            )
            .assertView('create-started', selector);
    });
});
