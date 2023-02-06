describe('Competitions---Controls', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Competitions%20%7C%20Controls&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
