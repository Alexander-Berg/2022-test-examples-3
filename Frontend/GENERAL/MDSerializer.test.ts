/* eslint-disable */
import serialize from './MDSerializer';

test('serialize mention correct', () => {
    const slate = [{ children: [{ type: 'p', children: [{ text: 'asdasd ' }, { type: 'mention', children: [{ text: '' }], value: 'ssav' }, { text: 'asdsad' }] }] }];

    expect(serialize(slate[0])).toBe('asdasd @ssav asdsad\n');
});

test('serialize blocks correct', () => {
    const slate = [{ children: [{ type: 'p', children: [{ text: 'Жирный ', bold: true }, { text: 'италик ', italic: true }, { text: 'подчеркнуты', italic: true, underline: true }] }, { type: 'p', children: [{ text: 'зачеркнуты', italic: true, strikethrough: true }, { text: ' ' }, { type: 'a', url: 'https://someurl.ru', children: [{ text: 'ссылка' }] }, { text: ' sss' }] }, { type: 'p', children: [{ text: 'код', code: true }] }, { type: 'ul', children: [{ type: 'li', children: [{ type: 'p', children: [{ text: 'список 1' }] }] }, { type: 'li', children: [{ type: 'p', children: [{ text: 'список 2' }] }] }, { type: 'li', children: [{ type: 'p', children: [{ text: 'список 3' }] }] }] }, { type: 'p', children: [{ text: '' }] }, { type: 'ol', children: [{ type: 'li', children: [{ type: 'p', children: [{ text: 'Номер список 1' }] }] }, { type: 'li', children: [{ type: 'p', children: [{ text: 'список 2' }] }] }] }, { type: 'p', children: [{ text: '' }] }, { type: 'blockquote', children: [{ text: 'цитата' }] }, { type: 'code_block', children: [{ text: 'Какой то код\nif (true) {\n  console.log(true)\n}' }] }, { type: 'p', children: [{ text: '' }] }] }];

    expect(serialize(slate[0])).toMatchSnapshot();
});
