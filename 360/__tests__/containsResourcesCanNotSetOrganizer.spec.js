import containsResourcesCanNotSetOrganizer from '../containsResourcesCanNotSetOrganizer';

describe('eventForm/utils/containsResourcesCanNotSetOrganizer', () => {
  test('должен возвращать true, если есть переговорка с resourceType недоступным для установки организатора', () => {
    const resources = [
      {
        officeId: 1,
        resource: null
      },
      {
        officeId: 2,
        resource: {
          email: 'room1@yandex-team.ru',
          resourceType: 'massage-room'
        }
      },
      {
        officeId: 3,
        resource: {
          email: 'room2@yandex-team.ru',
          resourceType: 'room'
        }
      }
    ];

    expect(containsResourcesCanNotSetOrganizer(resources)).toBe(true);
  });

  test('должен возвращать true, если есть переговорка с type недоступным для установки организатора', () => {
    const resources = [
      {
        officeId: 1,
        resource: null
      },
      {
        officeId: 2,
        resource: {
          email: 'room1@yandex-team.ru',
          type: 'massage-room'
        }
      },
      {
        officeId: 3,
        resource: {
          email: 'room2@yandex-team.ru',
          type: 'room'
        }
      }
    ];

    expect(containsResourcesCanNotSetOrganizer(resources)).toBe(true);
  });

  test('должен возвращать false, если все переговорки доступны для установки организатора', () => {
    const resources = [
      {
        officeId: 1,
        resource: null
      },
      {
        officeId: 2,
        resource: {
          email: 'room1@yandex-team.ru',
          type: 'room'
        }
      },
      {
        officeId: 3,
        resource: {
          email: 'room2@yandex-team.ru',
          resourceType: 'room'
        }
      }
    ];

    expect(containsResourcesCanNotSetOrganizer(resources)).toBe(false);
  });
});
