describeBlock('weather-forecast__tile', function(block) {
    var context,
        dataset = {},
        day,
        dayToday;

    stubBlocks([
        'weather-forecast__extended-url',
        'weather-forecast__set-sign',
        'weather-forecast__icon',
        'counter'
    ]);

    beforeEach(function() {
        context = {
            reportData: stubData('counters'),
            defaultCounter: { path: '/' }
        };

        day = {
            date: '2016-04-03T00:00:00+0500',
            parts: [
                {
                    temperature: 3,
                    type: 'night_short'
                },
                {
                    temperature: 9,
                    'temperature-data': {
                        avg: {
                            bgcolor: 'f4f1e0'
                        }
                    },
                    type: 'day_short'
                }
            ]
        };

        dayToday = {
            active: true,
            date: '2016-04-03T00:00:00+0500',
            parts: [
                {
                    temperature: 3,
                    type: 'night_short'
                },
                {
                    temperature: 9,
                    'temperature-data': {
                        avg: {
                            bgcolor: 'f4f1e0'
                        }
                    },
                    type: 'day_short'
                }
            ]
        };

        blocks['counter'].returns({});
    });

    it('should return `today` title for day is active and index === 0', function() {
        assert.equal(block(context, dataset, dayToday, 0).content[0].content, 'сегодня');
    });

    it('should return formatted string for title', function() {
        assert.equal(block(context, dataset, dayToday, 1).content[0].content, 'вс 3');
        assert.equal(block(context, dataset, day, 0).content[0].content, 'вс 3');
        assert.equal(block(context, dataset, day, 1).content[0].content, 'вс 3');
    });
});
