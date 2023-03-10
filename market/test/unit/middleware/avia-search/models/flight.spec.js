// TODO: Move stubs to separate files
const sinon = require('sinon');

const {
    FLIGHT_KEYS,
    COMPANY_IDS,
    concatenateFlights,
    validateFlight,
    parseFlight,
} = require('./../../../../../middleware/avia-search/models/flight/index');

describe('Flight', () => {
    const FLIGHTS = require('./flights.json');
    const NOT_VALID_FLIGHTS = require('./not-valid-flights.json');

    describe('validator', () => {
        test('should return null if flight is valid', () => {
            FLIGHTS &&
                Array.isArray(FLIGHTS) &&
                FLIGHTS.forEach((data) => {
                    const flight = data.flight;
                    const errors = validateFlight(flight);

                    expect(errors).toBeUndefined();
                });
        });

        test('should return errors is flight is not valid', () => {
            NOT_VALID_FLIGHTS &&
                Array.isArray(NOT_VALID_FLIGHTS) &&
                NOT_VALID_FLIGHTS.forEach((data) => {
                    const flight = data.flight;
                    const errors = validateFlight(flight);

                    expect(typeof errors).toBe('string');
                });
        });
    });

    describe('parser', () => {
        test('should parse correctly', () => {
            FLIGHTS &&
                Array.isArray(FLIGHTS) &&
                FLIGHTS.forEach((data) => {
                    const flight = data.flight;
                    const expected = data.expected;
                    const actual = parseFlight(flight);

                    expect(actual).toMatchObject(expected);
                });
        });

        test('should return undefined if variant is not valid', () => {
            NOT_VALID_FLIGHTS &&
                Array.isArray(NOT_VALID_FLIGHTS) &&
                NOT_VALID_FLIGHTS.forEach((data) => {
                    const flight = data.flight;
                    const actual = parseFlight(flight);

                    expect(actual).toBeUndefined();
                });
        });
    });

    describe('flights concatenator', () => {
        test('should concatenate correctly if count of flights is equal 1', () => {
            const flight = {
                [FLIGHT_KEYS]: ['S7 4066|MAD|DME|2016-11-07T23:35|2016-11-08T06:20'],
                [COMPANY_IDS]: [123],

                depart: {
                    airport: '??????????????',
                    iata: 'MAD',
                    city: '????????????',
                    date: '7 ????????????',
                    time: '23:35',
                    dateTime: '2016-11-07T23:35:00Z',
                },
                arrive: {
                    airport: '????????????????????',
                    iata: 'DME',
                    city: '????????????',
                    date: '9 ????????????',
                    time: '06:20',
                    dateTime: '2016-11-09T06:20:00Z',
                },
                duration: {},
            };

            const expected = flight;
            const actual = concatenateFlights([flight], []);

            expect(actual).toEqual(expected);
            expect(actual[FLIGHT_KEYS]).toEqual(expected[FLIGHT_KEYS]);
            expect(actual[COMPANY_IDS]).toEqual(expected[COMPANY_IDS]);
        });

        test('should concatenate correctly if count of flights is equal 2', () => {
            const first = {
                [FLIGHT_KEYS]: ['S7 4066|MAD|DME|2016-11-07T23:35|2016-11-08T06:20'],
                [COMPANY_IDS]: [123],

                depart: {
                    airport: '??????????????',
                    iata: 'MAD',
                    city: '????????????',
                    date: '7 ????????????',
                    time: '23:35',
                    dateTime: '2016-11-07T23:35:00Z',
                },
                arrive: {
                    airport: '????????????????????',
                    iata: 'DME',
                    city: '????????????',
                    date: '9 ????????????',
                    time: '06:20',
                    dateTime: '2016-11-09T06:20:00Z',
                },
                duration: {
                    days: 1,
                    diff: 162600000,
                    hours: 21,
                    minutes: 10,
                },
            };

            const second = {
                [FLIGHT_KEYS]: ['S7 4066|MAD|DME|2016-11-09T18:45|2016-11-09T20:45'],
                [COMPANY_IDS]: [456],

                depart: {
                    airport: '????????????????',
                    iata: 'BRU',
                    city: '????????????????',
                    dateTime: '2016-11-09T18:45:00Z',
                },
                arrive: {
                    airport: '??????-????????',
                    iata: 'BCN',
                    city: '??????????????????',
                    dateTime: '2016-11-09T20:45:00Z',
                },
                duration: {},
            };

            const firstTransfer = {
                duration: '14 ?? 25 ??????',
                text: '?????????????????? ????????????????',
                short_text: '??????????????????',
                type: 'transfer',
            };

            const expected = {
                [FLIGHT_KEYS]: [...first[FLIGHT_KEYS], ...second[FLIGHT_KEYS]],
                [COMPANY_IDS]: [...first[COMPANY_IDS], ...second[COMPANY_IDS]],

                depart: first.depart,
                arrive: second.arrive,
                duration: {
                    days: 1,
                    diff: 162600000,
                    hours: 21,
                    minutes: 10,
                },
                changesInfo: {
                    changes: [
                        {
                            duration: {
                                days: 0,
                                diff: 44700000,
                                hours: 12,
                                minutes: 25,
                            },
                            shortText: '??????????????????',
                            text: '?????????????????? ????????????????',
                            type: 'transfer',
                        },
                    ],
                    duration: {
                        days: 0,
                        diff: 44700000,
                        hours: 12,
                        minutes: 25,
                    },
                },
            };

            const actual = concatenateFlights([first, second], [firstTransfer]);

            expect(actual).toEqual(expected);
            expect(actual[FLIGHT_KEYS]).toEqual(expected[FLIGHT_KEYS]);
            expect(actual[COMPANY_IDS]).toEqual(expected[COMPANY_IDS]);
        });

        test('should concatenate correctly if count of flights is equal 3', () => {
            const first = {
                [FLIGHT_KEYS]: ['S7 4066|MAD|DME|2016-11-07T23:35|2016-11-08T06:20'],
                [COMPANY_IDS]: [123],

                depart: {
                    airport: '??????????????',
                    iata: 'MAD',
                    city: '????????????',
                    date: '7 ????????????',
                    time: '23:35',
                    dateTime: '2016-11-07T23:35+0100',
                },
                arrive: {
                    airport: '????????????????????',
                    iata: 'DME',
                    city: '????????????',
                    date: '9 ????????????',
                    time: '06:20',
                    dateTime: '2016-11-09T06:20+0300',
                },
                duration: '1 ?? 4 ?? 45 ??????',
            };

            const second = {
                [FLIGHT_KEYS]: ['S7 4066|MAD|DME|2016-11-09T18:45|2016-11-09T20:45'],
                [COMPANY_IDS]: [456],

                depart: {
                    airport: '????????????????',
                    iata: 'BRU',
                    city: '????????????????',
                    date: '9 ????????????',
                    time: '18:45',
                    dateTime: '2016-11-09T18:45+0100',
                },
                arrive: {
                    airport: '??????-????????',
                    iata: 'BCN',
                    city: '??????????????????',
                    date: '9 ????????????',
                    time: '20:45',
                    dateTime: '2016-11-09T20:45+0100',
                },
                duration: '2 ??',
            };

            const firstTransfer = {
                duration: '14 ?? 25 ??????',
                text: '???????????? ?????????????????? ????????????????',
                short_text: '???????????? ??????????????????',
                type: 'night_transfer',
            };

            const third = {
                [FLIGHT_KEYS]: ['LX 1324|ZRH|DME|2016-11-09T21:00|2016-11-11T02:25'],
                [COMPANY_IDS]: [456],

                depart: {
                    airport: '??????????',
                    iata: 'ZRH',
                    city: '??????????',
                    date: '9 ????????????',
                    time: '21:00',
                    dateTime: '2016-11-09T21:00+0100',
                },
                arrive: {
                    airport: '????????????????????',
                    iata: 'DME',
                    city: '????????????',
                    date: '11 ????????????',
                    time: '02:25',
                    dateTime: '2016-11-11T02:25+0100',
                },
                duration: '3 ?? 25 ??????',
            };

            const secondTransfer = {
                duration: '15 ??????',
                text: '?????????????????? ????????????????',
                short_text: '??????????????????',
                type: 'transfer',
            };

            const expected = {
                [FLIGHT_KEYS]: [...first[FLIGHT_KEYS], ...second[FLIGHT_KEYS], ...third[FLIGHT_KEYS]],
                [COMPANY_IDS]: [...first[COMPANY_IDS], ...second[COMPANY_IDS]],

                depart: first.depart,
                arrive: third.arrive,
                duration: {
                    days: 3,
                    diff: 269400000,
                    hours: 2,
                    minutes: 50,
                },
                changesInfo: {
                    changes: [
                        {
                            duration: {
                                days: 0,
                                diff: 51900000,
                                hours: 14,
                                minutes: 25,
                            },
                            shortText: '???????????? ??????????????????',
                            text: '???????????? ?????????????????? ????????????????',
                            type: 'night_transfer',
                        },
                        {
                            duration: {
                                days: 0,
                                diff: 900000,
                                hours: 0,
                                minutes: 15,
                            },
                            shortText: '??????????????????',
                            text: '?????????????????? ????????????????',
                            type: 'transfer',
                        },
                    ],
                    duration: {
                        days: 0,
                        diff: 52800000,
                        hours: 14,
                        minutes: 40,
                    },
                },
            };

            const actual = concatenateFlights([first, second, third], [firstTransfer, secondTransfer]);

            expect(actual).toEqual(expected);
            expect(actual[FLIGHT_KEYS]).toEqual(expected[FLIGHT_KEYS]);
            expect(actual[COMPANY_IDS]).toEqual(expected[COMPANY_IDS]);
        });
    });
});
