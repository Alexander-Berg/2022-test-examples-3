describeBlock('i-date', function() {
    describe('BEM.I18N', function() {
        it('`strftime` method should exist', function() {
            assert.ok(BEM.I18N.strftime);
        });
    });
});

describeBlock('i-date__correct-by-time-zone', function(block) {
    let context;

    const setTz = tz => {
        context = {
            reportData: {
                reqdata: {
                    user_time: {
                        tz
                    }
                }
            }
        };
    };

    it('subtract 10 hours"', () => {
        setTz('America/Los_Angeles');

        assert.equal(block(context, '2018-03-27T10:10:58'), '2018-03-27T00:10:58');
    });

    it('add 7 hours', () => {
        setTz('Asia/Vladivostok');

        assert.equal(block(context, '2018-03-27T10:10:58'), '2018-03-27T17:10:58');
    });

    it('add 5 hours', () => {
        setTz('Asia/Vladivostok');

        assert.equal(block(context, '2018-03-27T10:10:58+0500'), '2018-03-27T15:10:58');
    });

    it('add 3 hours', () => {
        setTz('Europe/Moscow');

        assert.equal(block(context, '2018-03-27T10:10:58+0000'), '2018-03-27T13:10:58');
    });
});

describeBlock('i-date__publication-time', function(block) {
    let newsTime;

    const setUserTime = userTime => ({
        userTime
    });

    beforeEach(() => {
        newsTime = '2018-03-26T10:10:58+1200';
    });

    it('should return "10:10 сегодня"', () => {
        assert.equal(block(setUserTime('2018-03-26T20:10:58+1200'), newsTime), '10:10 сегодня');
    });

    it('should return "10:10"', () => {
        assert.equal(block(setUserTime('2018-03-26T20:10:58+1200'), newsTime, true), '10:10');
    });

    it('should return "26 мар 2018"', () => {
        assert.equal(block(setUserTime('2018-05-26T10:10:58+1200'), newsTime), '26 мар 2018');
    });

    it('should work correct on CDate input', function() {
        const date = { epoch: '1483192800', __package: 'YxWeb::Util::CDate::Lazy', tz: 'GMT', __is_plain: 1 };

        assert.equal(block(setUserTime('2018-05-26T10:10:58+1200'), date), '31 дек 2016');
    });

    it('should not return current year', () => {
        assert.equal(block(setUserTime('2018-05-30T10:10:58+1200'), newsTime), '26 мар 2018');
    });
});
