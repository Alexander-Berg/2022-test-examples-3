describeBlock('i-entity-helpers__music_link-to-ya-music', function(block) {
    let context;
    let snippet;

    beforeEach(function() {
        context = { tld: 'ru', expFlags: {} };
        snippet = { base_info: {
            type: '',
            ugc_type: '',
            search_request: 'Paint It Black The Rolling Stones'
        } };
    });

    it('does not double encode text url', function() {
        assert.equal(block(context, snippet), 'https://music.yandex.ru/search?from=serp_autoplay&text=Paint%20It%20Black%20The%20Rolling%20Stones');
    });
});
