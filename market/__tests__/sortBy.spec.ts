/* eslint-disable no-unused-expressions */

import {sortBy} from '..';

const albums = [
    {title: 'Art of the Fugue', artist: 'Glenn Gould', genre: 'Baroque'},
    {title: 'A Farewell to Kings', artist: 'Rush', genre: 'Rock'},
    {title: 'Timeout', artist: 'Dave Brubeck Quartet', genre: 'Jazz'},
    {title: 'Fly By Night', artist: 'Rush', genre: 'Rock'},
    {title: 'Goldberg constiations', artist: 'Daniel Barenboim', genre: 'Baroque'},
    {title: 'New World Symphony', artist: 'Leonard Bernstein', genre: 'Romantic'},
    {title: 'Romance with the Unseen', artist: 'Don Byron', genre: 'Jazz'},
    {title: 'Somewhere In Time', artist: 'Iron Maiden', genre: 'Metal'},
    {title: 'In Times of Desparation', artist: 'Danny Holt', genre: 'Modern'},
    {title: 'Evita', artist: 'constious', genre: 'Broadway'},
    {title: 'Five Leaves Left', artist: 'Nick Drake', genre: 'Folk'},
    {title: 'The Magic Flute', artist: 'John Eliot Gardiner', genre: 'Classical'},
];

const eq = (a, b) => expect(a).toEqual(b);

describe('функция sortBy', () => {
    it('сортирует элементы по значению из функции', () => {
        const sortedAlbums = sortBy(item => item.title, albums);
        eq(sortedAlbums.length, albums.length);
        eq(sortedAlbums[0].title, 'A Farewell to Kings');
        eq(sortedAlbums[11].title, 'Timeout');

        (sortedAlbums as typeof albums);
    });

    it('сохраняет объекты', () => {
        const a = {value: 'a'};
        const b = {value: 'b'};
        const result = sortBy(item => item.value, [b, a]);
        eq(result[0], a);
        eq(result[1], b);
    });

    it('проверяет совместимость предиката и элементов', () => {
        const a = {value: 'a'};
        const b = {value: 'b'};

        // @ts-expect-error
        sortBy(item => item.nonExistingField, [b, a]);
        // @ts-expect-error
        sortBy(item => item.nonExistingField)([b, a]);
    });

    it('кидает исключение, если был передан не массив в качестве второго аргумента', () => {
        function sortByWithObject() {
            // @ts-expect-error
            sortBy(x => x, {a: 123});
            // @ts-expect-error
            sortBy(x => x)({a: 123});
        }
        function sortByWithNull() {
            sortBy(x => x, null);
            sortBy(x => x)(null);
        }
        function sortByWithFunction() {
            // @ts-expect-error
            sortBy(x => x, () => ({}));
            // @ts-expect-error
            sortBy(x => x)(() => ({}));
        }
        function sortByWithSymbol() {
            // @ts-expect-error
            sortBy(x => x, Symbol('some test symbol'));
            // @ts-expect-error
            sortBy(x => x)(Symbol('some test symbol'));
        }
        function sortByWithBoolean() {
            // @ts-expect-error
            sortBy(x => x, true);
            // @ts-expect-error
            sortBy(x => x)(true);
        }
        function sortByWithString() {
            // @ts-expect-error
            sortBy(x => x[0], 'string');
            // @ts-expect-error
            sortBy(x => x[0])('string');
        }

        expect(sortByWithObject).toThrow();
        expect(sortByWithNull).toThrow();
        expect(sortByWithFunction).toThrow();
        expect(sortByWithSymbol).toThrow();
        expect(sortByWithBoolean).toThrow();
        expect(sortByWithString).toThrow();
    });
});
