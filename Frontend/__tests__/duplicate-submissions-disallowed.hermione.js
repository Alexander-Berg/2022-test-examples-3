describe('Contest-Settings---Submissions', function() {
    it('duplicate-submissions-disallowed', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Submissions&selectedStory=duplicate-submissions-disallowed',
            )
            .assertView('duplicate-submissions-disallowed', selector);
    });
});
