describeBlock('i-format-duration', function(block) {
    every([undefined, 0, '', null, false], 'should return empty string if duration is falsy', function(duration) {
        let result = block(duration);

        assert.strictEqual(result, '');
    });

    it('should round non-integer seconds to the upper number', function() {
        let result = block(127.84);

        assert.strictEqual(result, '02:08');
    });

    it('should round non-integer seconds to the lower number', function() {
        let result = block(127.49);

        assert.strictEqual(result, '02:07');
    });

    describe('minutes with leading zero', function() {
        const durations = {
            '1': '00:01',
            '9': '00:09',
            '10': '00:10',
            '59': '00:59',
            '60': '01:00',
            '61': '01:01',
            '3599': '59:59',
            '3600': '1:00:00',
            '3601': '1:00:01',
            '10000': '2:46:40'
        };

        every(Object.keys(durations), 'should return correct formatted time', function(duration) {
            let result = block(Number(duration));

            assert.strictEqual(result, durations[duration]);
        });
    });

    describe('minutes without leading zero', function() {
        const durations = {
            '1': '00:01',
            '9': '00:09',
            '10': '00:10',
            '59': '00:59',
            '60': '1:00',
            '61': '1:01',
            '3599': '59:59',
            '3600': '1:00:00',
            '3601': '1:00:01',
            '10000': '2:46:40'
        };

        every(Object.keys(durations), 'should return correct formatted time', function(duration) {
            let result = block(Number(duration), true);

            assert.strictEqual(result, durations[duration]);
        });
    });
});

describeBlock('i-format-duration__convert-duration', function(block) {
    every([undefined, 0, '', null, false], 'should return undefined if duration is falsy', function(duration) {
        let result = block(duration);

        assert.strictEqual(result, undefined);
    });

    it('should round non-integer seconds to the lower number of minutes', function() {
        let result = block(127);

        assert.deepEqual(result, {
            minutes: 2,
            hours: 0,
            days: 0
        });
    });

    const durations = {
        '1': { minutes: 0, hours: 0, days: 0 },
        '9': { minutes: 0, hours: 0, days: 0 },
        '59': { minutes: 0, hours: 0, days: 0 },
        '60': { minutes: 1, hours: 0, days: 0 },
        '61': { minutes: 1, hours: 0, days: 0 },
        '3599': { minutes: 59, hours: 0, days: 0 },
        '3600': { minutes: 0, hours: 1, days: 0 },
        '3601': { minutes: 0, hours: 1, days: 0 },
        '10000': { minutes: 46, hours: 2, days: 0 },
        '87900': { minutes: 0, hours: 1, days: 1 }
    };

    every(Object.keys(durations), 'should return correct formatted time', function(duration) {
        let result = block(Number(duration));

        assert.deepEqual(result, durations[duration]);
    });

    it('should not clear minutes', function() {
        assert.deepEqual(block(87900, { saveMinutes: true }), {
            days: 1,
            hours: 0,
            minutes: 25
        });
    });
});
