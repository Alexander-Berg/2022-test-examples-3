describeBlock('adapter-entity-card__pop-tracks__ya-music-link', function(block) {
    var context, snpData, topTracksDataset;

    beforeEach(function() {
        context = {
            tld: 'ru',
            defaultCounter: {}
        };
        snpData = {
            base_info: {
                type: 'unexpectedType'
            }
        };
        topTracksDataset = {
            search_request: 'muse'
        };
    });

    it('should return search url, when an unexpected object type is received', function() {
        assert.strictEqual(block(context, snpData, topTracksDataset), 'https://music.yandex.ru/search?from=serp_autoplay&text=muse');
    });
});
