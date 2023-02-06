describe('Button', () => {
    it('default', function () {
        return this.browser
            .setViewportSize({ width: 800, height: 600 })
            .openComponent('components', 'button', 'default')
            .assertView('plain', ['[data-test="button"]']);
    });
});
