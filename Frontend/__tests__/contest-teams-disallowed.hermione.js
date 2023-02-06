describe('Contest-Settings---Access', function() {
    it('contest-teams-disallowed', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Access&selectedStory=contest-teams-disallowed',
            )
            .assertView('contest-teams-disallowed', selector);
    });
});
