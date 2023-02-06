describe('Problemsets---Controls', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemsets%20%7C%20Controls&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
