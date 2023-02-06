import {getEventCloneUrl} from '../getEventCloneUrl';

describe('eventForm/utils/getEventCloneUrl', () => {
  describe('url удовлетворяет требования длины', () => {
    test('должен работать', () => {
      const event = {
        start: 1,
        end: 2,
        isAllDay: false,
        attendees: [{email: 'tet4enko@yandex-team.ru'}, {email: 'tavria@yandex-team.ru'}],
        optionalAttendees: [{email: 'birhoff@yandex-team.ru'}],
        organizer: 'dmirain@yandex-team.ru',
        resources: [{email: 'cherdak@yandex-team.ru'}],
        name: 'name',
        description: 'description',
        othersCanView: false
      };

      expect(getEventCloneUrl(event)).toBe(
        // eslint-disable-next-line max-len
        '/event?attendees=tet4enko%40yandex-team.ru&attendees=tavria%40yandex-team.ru&description=description&endTs=2&isAllDay=0&name=name&optionalAttendees=birhoff%40yandex-team.ru&othersCanView=0&startTs=1'
      );
    });
  });

  describe('url не удовлетворяет требования длины', () => {
    test('в первую очередь должен отрезать описание', () => {
      const event = {
        start: 1,
        end: 2,
        isAllDay: false,
        attendees: [{email: 'tet4enko@yandex-team.ru'}, {email: 'tavria@yandex-team.ru'}],
        optionalAttendees: [{email: 'birhoff@yandex-team.ru'}],
        organizer: 'dmirain@yandex-team.ru',
        resources: [{email: 'cherdak@yandex-team.ru'}],
        name: 'name',
        description:
          // eslint-disable-next-line max-len
          'descriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescription',
        othersCanView: false
      };

      expect(getEventCloneUrl(event)).toBe(
        // eslint-disable-next-line max-len
        '/event?attendees=tet4enko%40yandex-team.ru&attendees=tavria%40yandex-team.ru&endTs=2&isAllDay=0&name=name&optionalAttendees=birhoff%40yandex-team.ru&othersCanView=0&startTs=1'
      );
    });

    test('во второую очередь должен отрезать опциональных участников', () => {
      const event = {
        start: 1,
        end: 2,
        isAllDay: false,
        attendees: [{email: 'tet4enko@yandex-team.ru'}, {email: 'tavria@yandex-team.ru'}],
        optionalAttendees: [
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'},
          {email: 'birhoff@yandex-team.ru'}
        ],
        organizer: 'dmirain@yandex-team.ru',
        resources: [{email: 'cherdak@yandex-team.ru'}],
        name: 'name',
        description:
          // eslint-disable-next-line max-len
          'descriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescriptiondescription',
        othersCanView: false
      };

      expect(getEventCloneUrl(event)).toBe(
        // eslint-disable-next-line max-len
        '/event?attendees=tet4enko%40yandex-team.ru&attendees=tavria%40yandex-team.ru&endTs=2&isAllDay=0&name=name&othersCanView=0&startTs=1'
      );
    });
  });
});
