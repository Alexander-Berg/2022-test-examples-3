const _ = require('underscore');

const extract = require('./../../../middleware/client-event/extract-client-event');
const format = require('./../../../middleware/client-event/format-client-event/format-client-event');

const extractAndFormat = _.compose((events) => events.map(format).filter((event) => !!event), extract);

describe('extract and format client events', () => {
    const body = { transaction_id: 'ijzm26lcqhste2hzxv10gx02gihdn7z4' };
    const settings = {
        clid: '100500',
        clientId: 'test',
        bucketInfo: {
            test: 'original',
        },
    };
    const cookies = {};

    describe('avia-prices', () => {
        test('should log avia-prices', () => {
            const formattedEvents = extractAndFormat({
                body: Object.assign(
                    {
                        interaction: 'avia-prices',
                        interaction_details: {
                            our: 100,
                            their: 500,
                        },
                    },
                    body,
                ),
                settings,
                cookies,
            });

            const actual = _.pick(
                formattedEvents[0],
                'event',
                'eventDetails',
                'avia-their-price',
                'avia-our-price',
                'avia-our-price',
                'detailsFields',
            );

            const expected = {
                event: 'avia-prices',
                eventDetails: { our: 100, their: 500 },
                'avia-their-price': 500,
                'avia-our-price': 100,
                detailsFields: ['avia-our-price', 'avia-their-price'],
            };

            expect(actual).toEqual(expected);
        });
    });

    describe('avia-time', () => {
        test('should log all possible types', () => {
            const time = [800, 100, 99, 4];

            time.forEach((time) => {
                const formattedEvents = extractAndFormat({
                    body: Object.assign(
                        {
                            interaction: 'avia-time',
                            interaction_details: time,
                        },
                        body,
                    ),
                    settings,
                    cookies,
                });

                expect(_.pick(formattedEvents[0], 'event', 'eventDetails')).toEqual({
                    event: 'avia-time',
                    eventDetails: time,
                });
            });
        });
    });

    describe('search-results-more', () => {
        test('should log all possible types', () => {
            const counts = [1, 2, 3, 10];

            counts.forEach((count) => {
                const formattedEvents = extractAndFormat({
                    body: Object.assign(
                        {
                            interaction: 'search-results-more',
                            interaction_details: count,
                        },
                        body,
                    ),
                    settings,
                    cookies,
                });

                expect(_.pick(formattedEvents[0], 'event', 'eventDetails')).toEqual({
                    event: 'search-results-more',
                    eventDetails: count,
                });
            });
        });
    });

    describe('search-results-more', () => {
        test('should log all possible types', () => {
            const counts = [1, 2, 3, 10];

            counts.forEach((count) => {
                const formattedEvents = extractAndFormat({
                    body: Object.assign(
                        {
                            interaction: 'search-results-more',
                            interaction_details: count,
                        },
                        body,
                    ),
                    settings,
                    cookies,
                });

                expect(_.pick(formattedEvents[0], 'event', 'eventDetails')).toEqual({
                    event: 'search-results-more',
                    eventDetails: count,
                });
            });
        });
    });

    describe('pricebar_close', () => {
        test('should log string eventDetails', () => {
            const formattedEvents = extractAndFormat({
                body: Object.assign(
                    {
                        interaction: 'pricebar_close',
                        interaction_details: JSON.stringify({
                            time: 20466,
                            popup_shown: 2,
                            popup_closed: 2,
                            popup_interaction: 1,
                        }),
                    },
                    body,
                ),
                settings,
                cookies,
            });

            const actual = _.pick(
                formattedEvents[0],
                'event',
                'eventDetails',
                'pricebar-close-time',
                'popup-shown-count',
                'popup-closed-count',
                'popup-integration-count',
                'detailsFields',
            );

            const expected = {
                event: 'pricebar_close',
                eventDetails: JSON.stringify({
                    time: 20466,
                    popup_shown: 2,
                    popup_closed: 2,
                    popup_interaction: 1,
                }),
                'pricebar-close-time': 20466,
                'popup-shown-count': 2,
                'popup-closed-count': 2,
                'popup-integration-count': 1,
                detailsFields: [
                    'pricebar-close-time',
                    'popup-shown-count',
                    'popup-closed-count',
                    'popup-integration-count',
                ],
            };

            expect(actual).toEqual(expected);
        });

        test('should log eventDetails', () => {
            const formattedEvents = extractAndFormat({
                body: Object.assign(
                    {
                        interaction: 'pricebar_close',
                        interaction_details: {
                            time: 20466,
                            popup_shown: 2,
                            popup_closed: 2,
                            popup_interaction: 1,
                        },
                    },
                    body,
                ),
                settings,
                cookies,
            });

            const actual = _.pick(
                formattedEvents[0],
                'event',
                'eventDetails',
                'pricebar-close-time',
                'popup-shown-count',
                'popup-closed-count',
                'popup-integration-count',
                'detailsFields',
            );

            const expected = {
                event: 'pricebar_close',
                eventDetails: {
                    time: 20466,
                    popup_shown: 2,
                    popup_closed: 2,
                    popup_interaction: 1,
                },
                'pricebar-close-time': 20466,
                'popup-shown-count': 2,
                'popup-closed-count': 2,
                'popup-integration-count': 1,
                detailsFields: [
                    'pricebar-close-time',
                    'popup-shown-count',
                    'popup-closed-count',
                    'popup-integration-count',
                ],
            };

            expect(actual).toEqual(expected);
        });
    });

    describe('notification_close', () => {
        test('should log string eventDetails', () => {
            const formattedEvents = extractAndFormat({
                body: Object.assign(
                    {
                        interaction: 'notification_close',
                        interaction_details: JSON.stringify({
                            status: 'firefox',
                            duration: 21000,
                        }),
                    },
                    body,
                ),
                settings,
                cookies,
            });

            expect(_.pick(formattedEvents[0], 'event', 'event_details', 'duration')).toEqual({
                event: 'notification_close',
                event_details: 'firefox',
                duration: 21000,
            });
        });

        test('should log eventDetails', () => {
            const formattedEvents = extractAndFormat({
                body: Object.assign(
                    {
                        interaction: 'notification_close',
                        interaction_details: {
                            status: 'firefox',
                            duration: 21000,
                        },
                    },
                    body,
                ),
                settings,
                cookies,
            });

            expect(_.pick(formattedEvents[0], 'event', 'event_details', 'duration')).toEqual({
                event: 'notification_close',
                event_details: 'firefox',
                duration: 21000,
            });
        });
    });
});
