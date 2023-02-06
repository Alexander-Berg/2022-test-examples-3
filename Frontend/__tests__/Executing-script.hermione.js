describe('Problems---Script', function() {
    it('Executing-script', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Script&selectedStory=Executing-script',
            )
            .assertView('Executing-script', selector, {
                ignoreElements: '.Button2_progress',
            });
    });
});
