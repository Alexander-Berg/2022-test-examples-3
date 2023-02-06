describe('Members-List---Mode-Select', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Members%20List%20%7C%20Mode%20Select&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
