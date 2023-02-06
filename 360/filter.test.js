'use strict';

const filter = require('./filter.js');

test('работает со всяким', () => {
    expect([ filter(), filter({}), filter([]) ]).toMatchSnapshot();
});

test('добавляет ref', () => {
    const rawEvent = {
        eventInfo: {
            externalId: '8AYnRtRJyandex.ru',
            name: 'test',
            location: '',
            description: '',
            start: 1572624000000,
            end: 1572625800000,
            isAllDay: false,
            latestInstanceTs: 1572625800000,
            organizer: {
                type: 'yandexUser',
                name: 'robot-mail-internal robot-mail-internal',
                email: 'robot-mail-internal@yandex-team.ru',
                decision: 'accepted',
                login: 'robot-mail-internal'
            },
            attendees: [
                {
                    type: 'yandexUser',
                    name: 'Vasya P.',
                    email: 'example@yandex-team.ru',
                    decision: 'undecided',
                    login: 'example'
                }
            ],
            startDt: '2019-11-01T19:00:00+03:00',
            endDt: '2019-11-01T19:30:00+03:00',
            decision: 'undecided',
            instanceKey: 'SX3JnyiFKQgPj/VEiOkSwQ==',
            isCancelled: false,
            isPast: true,
            isNoRsvp: false,
            actions: [],
            calendarMailType: 'event_invitation',
            calendarUrl: 'http://calendar.testing.yandex-team.ru/event?event_id=153203',
            prodId: '-//Yandex LLC//Yandex Calendar//EN'
        }
    };

    expect(filter(rawEvent)).toMatchSnapshot();
});
