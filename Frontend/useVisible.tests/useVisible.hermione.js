describe('react-hooks_useVisible', () => {
    it('should change the visibility of a component', function() {
        return this.browser
            .openComponent('react-hooks', 'usevisible', 'states')
            .setViewportSize({ width: 1000, height: 500 })
            .assertView('plain', ['.TestValueComponent'])
            .scroll('.ScrollToComponent')
            .assertView('change', ['.AnchorValueComponent']);
    });
});
