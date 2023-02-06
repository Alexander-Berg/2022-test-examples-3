describe('Confirmation-Modal', function() {
    it('custom-button', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Confirmation%20Modal&selectedStory=custom-button',
            )
            .assertView('custom-button', selector);
    });
});
