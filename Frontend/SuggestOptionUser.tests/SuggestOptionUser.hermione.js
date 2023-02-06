describe('tools-components_SuggestOptionUser', () => {
    it('default', function() {
        return (
            this.browser
                .openComponent('tools-components', 'suggestoptionuser-desktop', 'playground')
                .assertView('plain', ['.SuggestOptionUser'])
        );
    });
});
