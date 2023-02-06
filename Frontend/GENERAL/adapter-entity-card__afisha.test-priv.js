describeBlock('adapter-entity-card__afisha-time', function(block) {
    const context = { reportData: { reqdata: { user_time: {} } } };
    const time = '2017-09-05T01:25:00';

    it('should add iso timezone to schedule time', function() {
        context.reportData.reqdata.user_time.to_iso = '2017-09-04T17:42:13+0300';

        assert.equal(block(context, time), '2017-09-05T01:25:00+0300');
    });
});

describeBlock('adapter-entity-card__afisha-schedule-places-url', function(block) {
    let context;
    let place;

    beforeEach(function() {
        context = {};
        place = { title: 'Galaxy Cinemas Barrie', url: '//afisha.yandex.ru/' };
    });

    it('should return undefined if cinema has no url', function() {
        place.url = undefined;

        assert.isUndefined(block(context, place));
    });

    it('should add params in cinema url', function() {
        assert.strictEqual(
            block(context, place),
            '//afisha.yandex.ru/?from=qa'
        );
    });
});
