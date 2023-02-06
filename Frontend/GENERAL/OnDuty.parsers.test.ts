import { parseOnDutyShifts, parsePersonOnDuty } from './OnDuty.parsers';

describe('OnDuty.parser', () => {
    const backendShift = {
        id: 123,
        is_approved: true,
        start: '2019-11-09',
        start_datetime: '2019-11-09T00:00:00+03:00',
        end: '2019-11-13',
        end_datetime: '2019-11-14T00:00:00+03:00',
        person: null,
        schedule: {
            id: 1,
            name: 'ScheduleName',
            slug: 'schedule-slug',
        },
    };

    const backendPerson = {
        id: 123,
        login: 'somelogin',
        first_name: {
            ru: 'Name',
            en: 'Name',
        },
        last_name: {
            ru: 'Last Name',
            en: 'Last Name',
        },
    };

    describe('parsePersonOnDuty', () => {
        it('Should convert fields to frontend format', () => {
            const parsedPerson = parsePersonOnDuty(backendPerson);
            const expected = {
                id: 123,
                login: 'somelogin',
                firstName: {
                    ru: 'Name',
                    en: 'Name',
                },
                lastName: {
                    ru: 'Last Name',
                    en: 'Last Name',
                },
            };
            expect(parsedPerson).toStrictEqual(expected);
        });
    });

    describe('parseOnDutyShifts', () => {
        it('Should convert fields to frontend format', () => {
            const parsedShifts = parseOnDutyShifts(backendShift);

            const expected = {
                id: 123,
                isApproved: true,
                start: '2019-11-09',
                startDatetime: '2019-11-09T00:00:00+03:00',
                end: '2019-11-13',
                endDatetime: '2019-11-14T00:00:00+03:00',
                person: null,
                schedule: {
                    id: 1,
                    name: 'ScheduleName',
                    slug: 'schedule-slug',
                },
            };

            expect(parsedShifts).toStrictEqual(expected);
        });

        it('Should convert nested person to frontend format', () => {
            const parsedShifts = parseOnDutyShifts({ ...backendShift, person: backendPerson });

            expect(parsedShifts.person).toStrictEqual(parsePersonOnDuty(backendPerson));
        });
    });
});
