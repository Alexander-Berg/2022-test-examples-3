describe('Contest-Settings---ParticipantInterface', function() {
    it('not-show-participants', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20ParticipantInterface&selectedStory=not-show-participants',
            )
            .assertView('not-show-participants', selector);
    });
});
