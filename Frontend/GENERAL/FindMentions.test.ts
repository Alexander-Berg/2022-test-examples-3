/* eslint-disable */
import { FindMentions } from './FindMentions';

test('find mentions', () => {
    const slate = [{ children: [{ type: 'p', children: [{ text: 'asdasd ' }, { type: 'mention', children: [{ text: '' }], value: 'ssav' }, { text: ' asdsad' }] }] }];

    expect(FindMentions(slate[0])).toStrictEqual(['ssav']);
});
