describe('ImagesGrid', () => {
    it('render', function() {
        return this.browser
            .openComponent('yandex-int-imagesgrid', 'imagesgrid-desktop', 'playground')
            .assertView('open', ['body']);
    });
});
