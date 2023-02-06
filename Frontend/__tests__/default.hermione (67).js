describe('Problems---Controls', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Controls&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
