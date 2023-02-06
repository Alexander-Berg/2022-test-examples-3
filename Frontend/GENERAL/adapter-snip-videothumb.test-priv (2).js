describeBlock('adapter-snip-videothumb__video-thumb', function(block) {
    const snippet = { film_id: 'id' };
    const context = { device: 'device' };

    stubBlocks([
        'adapter-snip-videothumb__video-thumb-url',
        'adapter-snip-videothumb__url',
        'i-format-duration'
    ]);

    it('should remove destination from counter vars', function() {
        assert.deepEqual(block(context, snippet, {}).counter.vars, {
            '-filmid': snippet.film_id
        });
    });
});
