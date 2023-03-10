import { splitToCharacters } from '../../../../lib/helpers/text';

describe('text helpers', () => {
    describe('splitToCharacters', () => {
        it('abcde', () => {
            expect(splitToCharacters('abcde')).toEqual(['a', 'b', 'c', 'd', 'e']);
        });

        it('ðað', () => {
            expect(splitToCharacters('ðað')).toEqual(['ð', 'a', 'ð']);
        });

        it('ððð', () => {
            expect(splitToCharacters('ððð')).toEqual(['ð', 'ð', 'ð']);
        });

        it('ððab', () => {
            expect(splitToCharacters('ððab')).toEqual(['ð', 'ð', 'a', 'b']);
        });
    });
});
