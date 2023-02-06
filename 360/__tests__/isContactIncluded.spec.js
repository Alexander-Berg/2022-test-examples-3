import isContactIncluded from '../isContactIncluded';

describe('abookSuggest/utils/isContactIncluded', () => {
  test('должен вернуть false, если нет контактов', () => {
    const email = 'test@yandex.ru';
    const members = [];

    expect(isContactIncluded(email, members)).toBe(false);
  });

  test('должен вернуть false, если нет контакта c переданным email', () => {
    const email = 'test@yandex.ru';
    const members = [
      {
        email: 'test1@yandex.ru'
      },
      {
        email: 'test2@yandex.ru'
      }
    ];

    expect(isContactIncluded(email, members)).toBe(false);
  });

  test('должен вернуть true, если есть контакт с переданным email', () => {
    const email = 'test2@yandex.ru';
    const members = [
      {
        email: 'test1@yandex.ru'
      },
      {
        email: 'test2@yandex.ru'
      }
    ];

    expect(isContactIncluded(email, members)).toBe(true);
  });
});
