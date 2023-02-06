import isNotificationsEqual from '../isNotificationsEqual';

describe('notificationsField/utils/isNotificationsEqual', () => {
  test('должен возвращать true, если не передали нотификации', () => {
    expect(isNotificationsEqual()).toBe(true);
  });

  test('должен возвращать true, если нет нотификаций', () => {
    const prevNotifications = [];
    const nextNotifications = [];

    expect(isNotificationsEqual(prevNotifications, nextNotifications)).toBe(true);
  });

  test('должен возвращать true, если нотификации одинаковые', () => {
    const prevNotifications = [
      {
        channel: 'email',
        offset: '-15m'
      }
    ];
    const nextNotifications = [
      {
        channel: 'email',
        offset: '-15m'
      }
    ];

    expect(isNotificationsEqual(prevNotifications, nextNotifications)).toBe(true);
  });

  test('должен возвращать true, если нотификации одинаковые, не считая порядка', () => {
    const prevNotifications = [
      {
        channel: 'email',
        offset: '-15m'
      },
      {
        channel: 'sms',
        offset: '-40m'
      }
    ];
    const nextNotifications = [
      {
        channel: 'sms',
        offset: '-40m'
      },
      {
        channel: 'email',
        offset: '-15m'
      }
    ];

    expect(isNotificationsEqual(prevNotifications, nextNotifications)).toBe(true);
  });

  test('должен возвращать false, если нотификации разные', () => {
    const prevNotifications = [
      {
        channel: 'email',
        offset: '-15m'
      }
    ];
    const nextNotifications = [
      {
        channel: 'email',
        offset: '-30m'
      }
    ];

    expect(isNotificationsEqual(prevNotifications, nextNotifications)).toBe(false);
  });
});
