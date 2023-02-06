import i18n from 'utils/i18n';

import getFormattedOffset from '../getFormattedOffset';

describe('getFormattedOffset', () => {
  describe('событие не на весь день', () => {
    test('должен возвращать "в момент события"', () => {
      expect(getFormattedOffset('0m', false)).toEqual(i18n.get('notices', 'atEventTime'));
    });

    test('должен возвращать "за N минут"', () => {
      expect(getFormattedOffset('-10m', false)).toEqual(
        i18n.get('notices', 'formattedOffset_inAdvance', {
          offset: 10,
          unit: i18n.get('notices', 'unit_minutes', {offset: 10})
        })
      );
    });
  });

  describe('событие на весь день', () => {
    test('должен возвращать "00:00 в день события"', () => {
      expect(getFormattedOffset('0m', true)).toEqual(
        i18n.get('notices', 'formattedOffset_atTimeOnEventDay', {time: '00:00'})
      );
    });

    test('должен возвращать "00:00 за 2 дня до события"', () => {
      expect(getFormattedOffset('-2880m', true)).toEqual(
        i18n.get('notices', 'formattedOffset_atTimeInAdvance', {
          time: '00:00',
          offset: 2,
          unit: i18n.get('notices', `unit_days`, {offset: 2})
        })
      );
    });
  });
});
