describeBlock('adapter-entity-card__freshness-trailer', function(block) {
    var context, trailerData;

    beforeEach(function() {
        context = { reportData: { reqdata: { user_time: { to_iso: '2017-10-18' } } } };
    });

    it('should return undefined if date anymore today', function() {
        trailerData = { date: '2017-10-19' };

        assert.isUndefined(block(context, trailerData));
    });

    it('should return today if date is 18 october', function() {
        trailerData = { date: '2017-10-18' };

        assert.nestedPropertyVal(block(context, trailerData), 'key', 'сегодня');
    });

    it('should return yesterday if date is 17 october', function() {
        trailerData = { date: '2017-10-17' };

        assert.nestedPropertyVal(block(context, trailerData), 'key', 'вчера');
    });

    it('should return "days ago" if date older than 2 day', function() {
        trailerData = { date: '2017-10-16' };

        assert.nestedPropertyVal(block(context, trailerData), 'key', '{count} дня назад');
    });

    it('should return "days ago" if date older than 6 day', function() {
        trailerData = { date: '2017-10-13' };

        assert.nestedPropertyVal(block(context, trailerData), 'key', '{count} дня назад');
    });

    it('should return undefined if date older than week', function() {
        trailerData = { date: '2017-10-10' };

        assert.isUndefined(block(context, trailerData));
    });
});

describeBlock('adapter-entity-card__utm', function(block) {
    let context = { templatePlatform: 'desktop' };
    let url = 'https://litres.ru/book/1';
    let content = 'ebook';
    let term = 'free';

    it('should return url marked with utm tags', function() {
        assert.equal(
            block(context, url, content, term),
            'https://litres.ru/book/1?utm_source=yandex&utm_medium=serp&utm_campaign=desktop&utm_content=ebook&utm_term=free'
        );
    });
});
