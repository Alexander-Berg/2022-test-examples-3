import { EMOJI } from '@mssngr/util';
import { modifier } from '../emoji/util';
import { emojies } from './emojies.data';

describe('text-formatter:emoji', () => {
    emojies.forEach(({ input, expect: expectedEmoji }) => {
        it(`${input} should be ${expectedEmoji}`, () => {
            let match = new RegExp(`([${EMOJI}])`).exec(input);

            if (!match) {
                throw new Error('Match not found');
            }

            expect(modifier(match, match).matches[0]).toBe(expectedEmoji);

            match = new RegExp(`([${EMOJI}])`).exec(`before ${input} after`);

            if (!match) {
                throw new Error('Match not found');
            }

            expect(modifier(match, match).matches[0]).toBe(expectedEmoji);
        });
    });
});
