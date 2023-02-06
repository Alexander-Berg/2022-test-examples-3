import config from 'configs/config';

import {isCorpEmail, isMyEmail} from '../emails';

describe('emails', () => {
  describe('isCorpEmail', () => {
    test('должен возвращать true, если домен yandex-team.ru', () => {
      expect(isCorpEmail('test@yandex-team.ru')).toBe(true);
    });

    test('должен возвращать true, если домен yandex-team.com', () => {
      expect(isCorpEmail('test@yandex-team.com')).toBe(true);
    });

    test('должен возращать true, если домен yandex-team.com.tr', () => {
      expect(isCorpEmail('test@yandex-team.com.tr')).toBe(true);
    });

    test('должен возращать true, если домен yandex-team.com.ua', () => {
      expect(isCorpEmail('test@yandex-team.com.ua')).toBe(true);
    });

    test('должен возвращать true, если домен yamoney.ru', () => {
      expect(isCorpEmail('test@yamoney.ru')).toBe(true);
    });

    test('должен возвращать true, если домен yamoney.сom', () => {
      expect(isCorpEmail('test@yamoney.com')).toBe(true);
    });

    test('должен возвращать true, если домен yamoney.сom.tr', () => {
      expect(isCorpEmail('test@yamoney.com.tr')).toBe(true);
    });

    test('должен возвращать true, если домен yamoney.com.ua', () => {
      expect(isCorpEmail('test@yamoney.com.ua')).toBe(true);
    });

    test('должен возвращать false, если передали некорповый домен', () => {
      expect(isCorpEmail('test@ya.ru')).toBe(false);
    });

    test('должен возращать false, если ничего передали пустое значение', () => {
      expect(isCorpEmail()).toBe(false);
    });
  });

  describe('isMyEmail', () => {
    beforeEach(() => {
      sinon.stub(config.user, 'emails').value([
        {
          address: 'test@yandex.ru',
          native: true
        },
        {
          address: 'test@ya.ru',
          native: true
        },
        {
          address: 'test@админкапдд.рф',
          native: false
        }
      ]);
    });

    test('должен возвращать true, если адрес содержится в списке адресов юзера и является внутренним', () => {
      expect(isMyEmail('test@ya.ru')).toBe(true);
    });

    test('должен возвращать false, если адрес содержится в списке адресов юзера, но не является внутренним', () => {
      expect(isMyEmail('test@админкапдд.рф')).toBe(false);
    });

    test('должен возвращать false, если адрес не содержится в списке адресов юзера', () => {
      expect(isMyEmail('test@yaa.ru')).toBe(false);
    });
  });
});
