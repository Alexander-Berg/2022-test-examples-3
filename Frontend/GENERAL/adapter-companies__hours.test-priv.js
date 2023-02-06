describeBlock('adapter-companies__opening-hours-short', function(block) {
    var context = {
        reportData: {
            reqdata: {
                user_time: {
                    to_iso: '2016-09-21T14:00:00.000Z'
                }
            }
        }
    };

    describe('Дата и время пользователя 17:00(+3) 21 сентября 2016 года', function() {
        describe('организация работает ежедневно', function() {
            var meta = {
                Hours: {
                    Availabilities: [
                        {
                            Everyday: 1,
                            Intervals: [{}]
                        }
                    ],
                    tzOffset: '10800'
                }
            };

            it('с 9 утра до полуночи', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '12:00:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '00:00:00';

                var result = block(context, meta);

                assert.equal(result, 'до 00:00');
            });

            it('c 8 утра до 16 дня', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '08:00:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '16:00:00';

                var result = block(context, meta);

                assert.equal(result, 'закрыто');
            });

            it('с 16 вечера до 5 утра следующего дня', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '16:00:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '05:00:00';

                var result = block(context, meta);

                assert.equal(result, 'до 05:00');
            });
        });

        describe('организация работает ежедневно', function() {
            var meta = {
                Hours: {
                    Availabilities: [
                        {
                            Everyday: 1,
                            TwentyFourHours: 1
                        }
                    ],
                    tzOffset: '10800'
                }
            };

            it('круглосуточно', function() {
                var result = block(context, meta);

                assert.equal(result, '24 часа');
            });
        });

        describe('организация будет работать завтра', function() {
            var meta = {
                Hours: {
                    Availabilities: [
                        {
                            Wednesday: 1,
                            Thursday: 1,
                            Intervals: [{}]
                        }
                    ],
                    tzOffset: '10800'
                }
            };

            it('с 9:30 утра по 6:30 следующего утра', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '09:30:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '06:30:00';

                var result = block(context, meta);

                assert.equal(result, 'до 06:30');
            });

            it('с 9:30 утра по 14:30 следующего утра', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '09:30:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '14:30:00';

                var result = block(context, meta);

                assert.equal(result, 'закрыто');
            });
        });

        describe('организация работала вчера', function() {
            var meta = {
                Hours: {
                    Availabilities: [
                        {
                            Tuesday: 1,
                            Intervals: [{}]
                        }
                    ],
                    tzOffset: '10800'
                }
            };

            it('с 09:30 утра до 06:30 сегодняшнего утра', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '09:30:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '06:30:00';

                var result = block(context, meta);

                assert.equal(result, 'закрыто');
            });

            it('с 23:30 вечера до 18:30 сегодняшнего вечера', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '23:30:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '18:30:00';

                var result = block(context, meta);

                assert.equal(result, 'до 18:30');
            });
        });

        describe('организация откроется завтра', function() {
            var meta = {
                Hours: {
                    Availabilities: [
                        {
                            Thursday: 1,
                            Intervals: [{}]
                        }
                    ],
                    tzOffset: '10800'
                }
            };

            it('в 09:30 утра по 18:30 вечера', function() {
                meta.Hours.Availabilities[0].Intervals[0].from = '09:30:00';
                meta.Hours.Availabilities[0].Intervals[0].to = '18:30:00';

                var result = block(context, meta);

                assert.equal(result, 'закрыто');
            });
        });
    });
});
