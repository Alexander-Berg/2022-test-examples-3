describe('tools-components_ToolsSuggest', () => {
    it('default', function() {
        return (
            this.browser
                .openComponent('tools-components', 'toolssuggest-desktop', 'playground')
                .assertView('plain', ['.ToolsSuggest'])
                .click('.Textinput-Control')
                .waitForVisible('.ToolsSuggest-Popup')
                .assertView('opened', ['.ToolsSuggest', '.ToolsSuggest-Popup'])
        );
    });
});
