describe('serp-header.web4.header-viewer', () => {
    it('viewer-is-open', function() {
        return this.browser
            .url('dist/examples/desktop/serp-header/serp-header.web4.base.html')
            .execute(function() {
                document.querySelector('.serp-header').classList.add('serp-header_viewer');
            })
            .assertView('header-viewer', '.serp-header');
    });
});
