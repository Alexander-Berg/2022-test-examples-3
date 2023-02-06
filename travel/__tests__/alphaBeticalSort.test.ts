import alphaBeticalSort from '../alphaBeticalSort';

describe('alphaBeticalSort', () => {
    it('Сортирует по алфавиту', () => {
        expect(
            ['b', 'a', 'ab', 'aa', 'A', '2', '1', '10'].sort(alphaBeticalSort),
        ).toEqual(['1', '10', '2', 'A', 'a', 'aa', 'ab', 'b']);
    });
});
