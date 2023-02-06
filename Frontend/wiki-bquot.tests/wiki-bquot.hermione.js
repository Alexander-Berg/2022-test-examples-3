describe('wiki-bqout', () => {
    it('wiki-bqout', function() {
        return this.browser
            .url('common.examples/wiki-bquot/wiki-bquot/wiki-bquot.html')
            .assertView('plain', '.wiki-bquot');
    });
});
