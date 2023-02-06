describe('ListTile', () => {
    it('should render component', function() {
        return this.browser
            .url('ListTile/hermione/hermione.html')
            .assertView('plain', ['.Hermione']);
    });
});
