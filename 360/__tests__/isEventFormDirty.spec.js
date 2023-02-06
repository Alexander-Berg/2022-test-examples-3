import Availability from 'constants/Availability';

import isEventFormDirty from '../isEventFormDirty';

describe('eventForm/utils/isEventFormDirty', () => {
  test('должен возвращать false, если нет initialValues', () => {
    const initialValues = null;
    const values = {};

    expect(isEventFormDirty(initialValues, values)).toBe(false);
  });

  test('должен возвращать false, если нет values', () => {
    const initialValues = {};
    const values = null;

    expect(isEventFormDirty(initialValues, values)).toBe(false);
  });

  test('должен возвращать true, если поменялось какое-нибудь поле', () => {
    const initialValues = {
      name: 'name',
      attendees: [],
      resources: [],
      notifications: []
    };
    const values = {
      name: 'NAME',
      attendees: [],
      resources: [],
      notifications: []
    };

    expect(isEventFormDirty(initialValues, values)).toBe(true);
  });

  describe('организатор', () => {
    test('должен возвращать true, если изменился организатор', () => {
      const initialValues = {
        organizer: {
          email: 'test@ya.ru'
        },
        attendees: [],
        resources: [],
        notifications: []
      };
      const values = {
        organizer: {
          email: 'test1@ya.ru'
        },
        attendees: [],
        resources: [],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(true);
    });
  });

  describe('участники', () => {
    test('должен возвращать true, если изменился список участников', () => {
      const initialValues = {
        attendees: [
          {
            email: 'test1@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };
      const values = {
        attendees: [
          {
            email: 'test2ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(true);
    });

    test('должен возвращать false, если в списке участников поменялся только порядок', () => {
      const initialValues = {
        attendees: [
          {
            email: 'test1@ya.ru'
          },
          {
            email: 'test2@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };
      const values = {
        attendees: [
          {
            email: 'test2@ya.ru'
          },
          {
            email: 'test1@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });

    test('должен возвращать false, если встреча 1х1, и у коллеги есть не-личный телеграм аккаунт', () => {
      const initialValues = {
        attendees: [
          {
            email: 'test1@ya.ru'
          },
          {
            email: 'test2@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };
      const values = {
        attendees: [
          {
            email: 'test2@ya.ru'
          },
          {
            email: 'test1@ya.ru'
          }
        ],
        resources: [],
        notifications: [],
        oneToOneWithCurrentUserOpponentTelegramAccount: 'sometgaccount'
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });

    test('должен возвращать false, если в списке участников не менялись email адреса', () => {
      const initialValues = {
        attendees: [
          {
            email: 'test1@ya.ru',
            availability: Availability.BUSY
          },
          {
            email: 'test2@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };
      const values = {
        attendees: [
          {
            email: 'test1@ya.ru',
            availability: Availability.AVAILABLE
          },
          {
            email: 'test2@ya.ru'
          }
        ],
        resources: [],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });
  });

  describe('переговорки', () => {
    test('должен возвращать true, если изменился список переговорок', () => {
      const initialValues = {
        attendees: [],
        resources: [
          {
            officeId: 2,
            resource: null
          }
        ],
        notifications: []
      };
      const values = {
        attendees: [],
        resources: [
          {
            officeId: 2,
            resource: {
              email: 'room1@ya.ru'
            }
          }
        ],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(true);
    });

    test('должен возвращать false, если в списке переговорок поменялся только порядок', () => {
      const initialValues = {
        attendees: [],
        resources: [
          {
            officeId: 2,
            resource: {
              email: 'room1@ya.ru'
            }
          },
          {
            office: 2,
            resource: {
              email: 'room2@ya.ru'
            }
          }
        ],
        notifications: []
      };
      const values = {
        attendees: [],
        resources: [
          {
            office: 2,
            resource: {
              email: 'room2@ya.ru'
            }
          },
          {
            officeId: 2,
            resource: {
              email: 'room1@ya.ru'
            }
          }
        ],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });

    test('должен возвращать false, если в списке переговорок не менялись email адреса', () => {
      const initialValues = {
        attendees: [],
        resources: [
          {
            officeId: 2,
            resource: {
              email: 'room1@ya.ru',
              availability: Availability.BUSY
            }
          },
          {
            office: 2,
            resource: {
              email: 'room2@ya.ru'
            }
          }
        ],
        notifications: []
      };
      const values = {
        attendees: [],
        resources: [
          {
            officeId: 2,
            resource: {
              email: 'room1@ya.ru',
              availability: Availability.AVAILABLE
            }
          },
          {
            office: 2,
            resource: {
              email: 'room2@ya.ru'
            }
          }
        ],
        notifications: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });
  });

  describe('уведомления', () => {
    test('должен возвращать true, если список уведомлений изменился', () => {
      const initialValues = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'email'
          }
        ]
      };
      const values = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'sms'
          }
        ]
      };

      expect(isEventFormDirty(initialValues, values)).toBe(true);
    });

    test('должен возвращать false, если список уведомлений не изменился ', () => {
      const initialValues = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'email'
          }
        ]
      };
      const values = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'email'
          }
        ]
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });

    test('не должен учитывать suggestedIntervals', () => {
      const initialValues = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'email'
          }
        ]
      };
      const values = {
        attendees: [],
        resources: [],
        notifications: [
          {
            offset: '-15m',
            channel: 'email'
          }
        ],
        suggestedIntervals: []
      };

      expect(isEventFormDirty(initialValues, values)).toBe(false);
    });
  });
});
