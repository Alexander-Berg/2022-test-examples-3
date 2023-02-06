import { splitToCharacters } from '../../../../lib/helpers/text';

describe('text helpers', () => {
    describe('splitToCharacters', () => {
        it('abcde', () => {
            expect(splitToCharacters('abcde')).toEqual(['a', 'b', 'c', 'd', 'e']);
        });

        it('🙂a👍', () => {
            expect(splitToCharacters('🙂a👍')).toEqual(['🙂', 'a', '👍']);
        });

        it('🙂🙂👍', () => {
            expect(splitToCharacters('🙂🙂👍')).toEqual(['🙂', '🙂', '👍']);
        });

        it('🙂👍ab', () => {
            expect(splitToCharacters('🙂👍ab')).toEqual(['🙂', '👍', 'a', 'b']);
        });
    });
});
