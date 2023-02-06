import extractMonogram from '../extractMonogram';

describe('avatars/utils/extractMonogram', () => {
  test('должен вернуть пустую строку, если ничего не передали', () => {
    expect(extractMonogram()).toBe('');
  });

  test('должен вернуть пустую строку, если передали строку из пробелов', () => {
    expect(extractMonogram(' ')).toBe('');
  });

  test('должен вернуть первую букву, если передали email', () => {
    expect(extractMonogram('test@ya.ru')).toBe('T');
  });

  test('должен вернуть исходную строку, если она состоит из двух символов', () => {
    expect(extractMonogram('db')).toBe('DB');
  });

  test('должен вернуть первую букву + первую заглавную, если заглавных несколько', () => {
    expect(extractMonogram('iBookStore')).toBe('IB');
  });

  test('должен вернуть заглавные буквы первых двух слов', () => {
    expect(extractMonogram('Ivan Ivanov')).toBe('II');
  });

  test('должен вернуть первую букву', () => {
    expect(extractMonogram('Ivanov')).toBe('I');
  });

  test('должен санитайзить лишние символы', () => {
    expect(extractMonogram('- _')).toBe('');
    expect(extractMonogram('-Переговорка')).toBe('П');
    expect(extractMonogram('?  Переговорка')).toBe('П');
  });
});
