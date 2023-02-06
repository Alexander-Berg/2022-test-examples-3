describe('Contest-Settings---Submissions', function() {
    it('counter-filters-checked', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Submissions&selectedStory=counter-filters-checked',
            )
            .assertView('counter-filters-checked', selector);
    });
});
