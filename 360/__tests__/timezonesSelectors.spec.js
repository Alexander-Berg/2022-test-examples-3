import {OrderedMap, Map} from 'immutable';

import SettingsRecord from 'features/settings/SettingsRecord';

import {getCurrentTimezone, getSortedTimezones} from '../timezonesSelectors';
import TimezoneRecord from '../TimezoneRecord';

describe('timezonesSelectors', () => {
  describe('getCurrentTimezone', () => {
    test('должен возвращать данные для текущей таймзоны', () => {
      const state = {
        timezones: new Map({
          'Europe/Moscow': new TimezoneRecord({
            id: 'Europe/Moscow',
            offset: -120
          }),
          'Europe/Minsk': new TimezoneRecord({
            id: 'Europe/Minsk',
            offset: 120
          })
        }),
        settings: new SettingsRecord({tz: 'Europe/Moscow'})
      };

      expect(getCurrentTimezone(state)).toEqual(
        new TimezoneRecord({
          id: 'Europe/Moscow',
          offset: -120
        })
      );
    });
  });

  describe('getSortedTimezones', () => {
    test('должен возвращать таймзоны, отсортированные по убыванию offset', () => {
      const state = {
        timezones: new Map({
          'Europe/Berlin': new TimezoneRecord({
            id: 'Europe/Berlin',
            offset: 0
          }),
          'Europe/Moscow': new TimezoneRecord({
            id: 'Europe/Moscow',
            offset: -120
          }),
          'Europe/Kiev': new TimezoneRecord({
            id: 'Europe/Kiev',
            offset: 240
          }),
          'Europe/Helsinki': new TimezoneRecord({
            id: 'Europe/Helsinki',
            offset: -180
          }),
          'Europe/Minsk': new TimezoneRecord({
            id: 'Europe/Minsk',
            offset: 120
          })
        })
      };

      expect(getSortedTimezones(state)).toEqual(
        new OrderedMap([
          ['Europe/Kiev', new TimezoneRecord({id: 'Europe/Kiev', offset: 240})],
          ['Europe/Minsk', new TimezoneRecord({id: 'Europe/Minsk', offset: 120})],
          ['Europe/Berlin', new TimezoneRecord({id: 'Europe/Berlin', offset: 0})],
          ['Europe/Moscow', new TimezoneRecord({id: 'Europe/Moscow', offset: -120})],
          ['Europe/Helsinki', new TimezoneRecord({id: 'Europe/Helsinki', offset: -180})]
        ])
      );
    });
  });
});
