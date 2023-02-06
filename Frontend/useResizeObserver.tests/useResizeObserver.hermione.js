describe('react-hooks_useResizeObserver', () => {
    it('should follow width and height changes of container', function() {
        return this.browser
            .openComponent('react-hooks', 'useresizeobserver', 'states')
            .assertView('plain', ['.Gemini', '.TestComponent'])
            .click('.ChangeButton')
            .assertView('change', ['.Gemini', '.TestComponent']);
    });
});
