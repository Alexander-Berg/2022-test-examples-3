import linkFormatter from '../utils/linkFormatter';

describe('patterns test', () => {
  const text_regular = '<a href="ya.ru">автопополнение</a>';
  const text_target_blank = '<a href="ya.ru" target="_blank">автопополнение</a>';
  const text_target_self = '<a href="ya.ru" target="_self" rel="wtf">автопополнение</a>';
  const text_target_other_0 = '<a title="Title" href="ya.ru">автопополнение</a>';
  const text_target_other_1 = '<a tabindex="-1" title="Title" href="ya.ru">автопополнение</a>';
  const text_target_other_2 = '<a href="ya.ru" tabindex="-1" title="Title">автопополнение</a>';
  const text_target_other_3 = '<a hreflang="ru" href="ya.ru" tabindex="-1" title="Title">автопополнение</a>';

  test('text default', () => {
    expect(linkFormatter(text_regular)).toBe('<a href="ya.ru" rel="noopener noreferrer" target="_blank">автопополнение</a>');
  });

  test('text blank', () => {
    expect(linkFormatter(text_target_blank)).toBe('<a href="ya.ru" rel="noopener noreferrer" target="_blank">автопополнение</a>');
  });

  test('text self', () => {
    expect(linkFormatter(text_target_self)).toBe('<a href="ya.ru" rel="noopener noreferrer" target="_blank">автопополнение</a>');
  });

  test('text other_0', () => {
    expect(linkFormatter(text_target_other_0)).toBe('<a title="Title" href="ya.ru" rel="noopener noreferrer" target="_blank">автопополнение</a>');
  });

  test('text other_1', () => {
    expect(linkFormatter(text_target_other_1)).toBe('<a tabindex="-1" title="Title" href="ya.ru" rel="noopener noreferrer" target="_blank">автопополнение</a>');
  });

  test('text other_2', () => {
    expect(linkFormatter(text_target_other_2)).toBe('<a href="ya.ru" rel="noopener noreferrer" target="_blank" tabindex="-1" title="Title">автопополнение</a>');
  });

  test('text other_3', () => {
    expect(linkFormatter(text_target_other_3)).toBe('<a hreflang="ru" href="ya.ru" rel="noopener noreferrer" target="_blank" tabindex="-1" title="Title">автопополнение</a>');
  });
});
