describeBlock('i-report-cdate', function(block) {
    it('should return undefined', function() {
        assert.isUndefined(block());
    });

    it('string date argument should return string date', function() {
        assert.equal(
            block('2018-06-25T23:13:26+0300Z'),
            '2018-06-25T23:13:26+0300Z',
        );
    });

    it('cdate object argument should return string date', function() {
        const blockTime = block({
            epoch: '1547017200',
            __package: 'YxWeb::Util::CDate::Lazy',
            tz: 'GMT',
            __is_plain: 1
        }).getTime();

        const refTime = new Date('2019-01-09T04:00:00.000Z').getTime();

        assert.isTrue(blockTime === refTime);
    });
});
