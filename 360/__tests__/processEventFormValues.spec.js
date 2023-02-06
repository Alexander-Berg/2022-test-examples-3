import moment from 'moment';

import {
  processName,
  processDates,
  processAttendeesAndResources,
  processUnnecessaryValues,
  processParticipantsCanEdit,
  processOrganizer,
  processRepetition
} from '../processEventFormValues';
import containsResourcesCanNotSetOrganizer from '../containsResourcesCanNotSetOrganizer';

jest.mock('utils/i18n');
jest.mock('../containsResourcesCanNotSetOrganizer');

describe('eventForm/utils/processEventFormValues', () => {
  describe('processName', () => {
    test('должен подставлять дефолтное название, если нет названия', () => {
      const values = {
        name: ''
      };
      const expectedValues = {
        name: 'event.withoutName'
      };

      processName(values);

      expect(values).toEqual(expectedValues);
    });

    test('должен подставлять дефолтное название, если название состоит из одних пробелов', () => {
      const values = {
        name: '    '
      };
      const expectedValues = {
        name: 'event.withoutName'
      };

      processName(values);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processDates', () => {
    test('должен обработать даты для обычного события', () => {
      const values = {
        start: Number(moment('2018-04-26T10:00:00')),
        end: Number(moment('2018-04-26T10:30:00')),
        isAllDay: false
      };
      const expectedValues = {
        start: '2018-04-26T10:00:00',
        end: '2018-04-26T10:30:00',
        isAllDay: false
      };

      processDates(values);

      expect(values).toEqual(expectedValues);
    });

    test('должен обработать даты для события на весь день', () => {
      const values = {
        start: Number(moment('2018-04-26T10:00:00')),
        end: Number(moment('2018-04-26T10:30:00')),
        isAllDay: true
      };
      const expectedValues = {
        start: '2018-04-26T00:00:00',
        end: '2018-04-27T00:00:00',
        isAllDay: true
      };

      processDates(values);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processAttendeesAndResources', () => {
    test('должен оставлять от участников и опциональных участников только их email адреса', () => {
      const values = {
        attendees: [
          {
            email: 'test@ya.ru'
          },
          {
            email: 'test1@ya.ru'
          }
        ],
        optionalAttendees: [
          {
            email: 'test3@ya.ru'
          },
          {
            email: 'test4@ya.ru'
          }
        ],
        resources: []
      };
      const expectedValues = {
        attendees: ['test@ya.ru', 'test1@ya.ru'],
        optionalAttendees: ['test3@ya.ru', 'test4@ya.ru']
      };

      processAttendeesAndResources(values);

      expect(values).toEqual(expectedValues);
    });

    test('должен добавлять email адреса переговорок в конец списка адресов участников', () => {
      const values = {
        attendees: [
          {
            email: 'test@ya.ru'
          }
        ],
        optionalAttendees: [
          {
            email: 'test2@ya.ru'
          }
        ],
        resources: [
          {
            officeId: 2,
            resource: {
              email: 'room@ya.ru'
            }
          }
        ]
      };
      const expectedValues = {
        attendees: ['test@ya.ru', 'room@ya.ru'],
        optionalAttendees: ['test2@ya.ru']
      };

      processAttendeesAndResources(values);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processParticipantsCanEdit', () => {
    test('должен брать текущее значение, если нет организатора', () => {
      const values = {
        organizer: null,
        participantsCanEdit: true,
        originalParticipantsCanEdit: false,
        organizerLetToEditAnyMeeting: false
      };
      const initialValues = {
        organizer: null,
        organizerLetToEditAnyMeeting: false
      };
      const expectedValues = {
        organizer: null,
        participantsCanEdit: true
      };

      processParticipantsCanEdit(values, initialValues);

      expect(values).toEqual(expectedValues);
    });

    test('должен брать текущее значение, если есть огранизатор, но у него выключена настройка', () => {
      const values = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: false,
        originalParticipantsCanEdit: true,
        organizerLetToEditAnyMeeting: false
      };
      const initialValues = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        organizerLetToEditAnyMeeting: false
      };
      const expectedValues = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: false
      };

      processParticipantsCanEdit(values, initialValues);

      expect(values).toEqual(expectedValues);
    });

    test('должен брать оригинальное значение, если есть организатор и у него стоит настройка', () => {
      const values = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: true,
        originalParticipantsCanEdit: false,
        organizerLetToEditAnyMeeting: true
      };
      const initialValues = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        organizerLetToEditAnyMeeting: true
      };
      const expectedValues = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: false
      };

      processParticipantsCanEdit(values, initialValues);

      expect(values).toEqual(expectedValues);
    });

    test('должен брать текущее значение, если организатор изменился', () => {
      const values = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: true,
        originalParticipantsCanEdit: false,
        organizerLetToEditAnyMeeting: true
      };
      const initialValues = {
        organizer: {
          email: 'test-2@yandex-team.ru'
        },
        organizerLetToEditAnyMeeting: true
      };
      const expectedValues = {
        organizer: {
          email: 'test-1@yandex-team.ru'
        },
        participantsCanEdit: true
      };

      processParticipantsCanEdit(values, initialValues);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processOrganizer', () => {
    test('должен добавлять email организатора, если это возможно', () => {
      containsResourcesCanNotSetOrganizer.mockReturnValue(false);

      const values = {
        organizer: {
          email: 'test@ya.ru'
        }
      };
      const expectedValues = {
        organizer: 'test@ya.ru'
      };
      const actions = {
        changeOrganizer: true
      };

      processOrganizer(values, actions);

      expect(values).toEqual(expectedValues);
    });

    test('не должен добавлять email организатора, если его нет', () => {
      containsResourcesCanNotSetOrganizer.mockReturnValue(false);

      const values = {
        organizer: null
      };
      const expectedValues = {};
      const actions = {
        changeOrganizer: true
      };

      processOrganizer(values, actions);

      expect(values).toEqual(expectedValues);
    });

    test('не должен добавлять email организатора, если нет на это прав', () => {
      containsResourcesCanNotSetOrganizer.mockReturnValue(false);

      const values = {
        organizer: {
          email: 'test@ya.ru'
        }
      };
      const expectedValues = {meta: {organizer: 'test@ya.ru'}};
      const actions = {
        changeOrganizer: false
      };

      processOrganizer(values, actions);

      expect(values).toEqual(expectedValues);
    });

    test('не должен добавлять email организатора, если есть переговорки в которых нельзя устанавливать организатора', () => {
      containsResourcesCanNotSetOrganizer.mockReturnValue(true);

      const values = {
        organizer: {
          email: 'test@ya.ru'
        }
      };
      const expectedValues = {meta: {organizer: 'test@ya.ru'}};
      const actions = {
        changeOrganizer: true
      };

      processOrganizer(values, actions);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processRepetition', () => {
    test('должен добавлять повторение, если оно включено', () => {
      const values = {
        repetition: {
          type: 'daily',
          daily: {
            every: 'everyWorkDay'
          }
        },
        repeat: true
      };
      const expectedValues = {
        repetition: {
          type: 'weekly',
          each: 1,
          weeklyDays: 'mon,tue,wed,thu,fri'
        }
      };

      processRepetition(values);

      expect(values).toEqual(expectedValues);
    });

    test('не должен добавлять повторение, если оно выключено', () => {
      const values = {
        repetition: {
          type: 'daily',
          daily: {
            every: 'everyWorkDay'
          }
        },
        repeat: false
      };
      const expectedValues = {};

      processRepetition(values);

      expect(values).toEqual(expectedValues);
    });
  });

  describe('processUnnecessaryValues', () => {
    test('должен удалять необязательные поля', () => {
      const values = {
        subscribers: [],
        resourcesFilter: {},
        totalAttendees: 100,
        ignoreUsersEvents: true,
        shouldCheckAvailability: true,
        suggestedIntervals: []
      };
      const expectedValues = {};

      processUnnecessaryValues(values);

      expect(values).toEqual(expectedValues);
    });
  });
});
