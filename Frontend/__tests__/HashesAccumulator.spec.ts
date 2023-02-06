import { HashesAccumulator, LOADED_HASHES_KEY } from '../HashesAccumulator';

function getArrOfStrings(limit: number): string[] {
    return new Array(limit).fill(null).map((_, idx) => idx.toString());
}

describe('Класс HashesAccumulator', () => {
    afterEach(() => {
        localStorage.clear();
    });

    test('метод add добавляет хеши в персистентное хранилище', () => {
        const accumulator = new HashesAccumulator();

        accumulator.add(['A', 'B', 'C']);

        const item = localStorage.getItem(LOADED_HASHES_KEY);
        const expected = JSON.stringify(['A', 'B', 'C']);

        expect(item).toEqual(expected);
    });

    test('метод add склеивает уже добавленные хеши с новыми', () => {
        const accumulator = new HashesAccumulator();

        accumulator.add(['A', 'B', 'C']);
        accumulator.add(['D', 'E', 'F']);

        const item = localStorage.getItem(LOADED_HASHES_KEY);
        const expected = JSON.stringify(['A', 'B', 'C', 'D', 'E', 'F']);

        expect(item).toEqual(expected);
    });

    test('метод getString возвращает все хеши в виде строки, разделённой запятыми', () => {
        const accumulator = new HashesAccumulator();

        localStorage.setItem(LOADED_HASHES_KEY, JSON.stringify(['HELLO', 'WORLD!']));

        expect(accumulator.getString()).toEqual('HELLO,WORLD!');
    });

    test('метод getString возвращает пустую строку, если хранилище пустое', () => {
        const accumulator = new HashesAccumulator();

        expect(accumulator.getString()).toEqual('');
    });

    test('методы add и getString работают вместе', () => {
        const accumulator = new HashesAccumulator();

        accumulator.add(['A', 'B', 'C']);
        expect(accumulator.getString()).toEqual('A,B,C');

        accumulator.add(['D', 'E', 'F']);
        expect(accumulator.getString()).toEqual('A,B,C,D,E,F');
    });

    test('длина сохранённых хешей не выходит за предел, если хранилище заполнено', () => {
        const arr = getArrOfStrings(400);
        const accumulator = new HashesAccumulator();

        accumulator.add(arr);
        const savedInitial = JSON.parse(localStorage.getItem(LOADED_HASHES_KEY));
        expect(savedInitial.length).toEqual(400);

        const newHashes = ['A', 'B', 'C', 'D'];
        accumulator.add(newHashes);

        const savedResult = JSON.parse(localStorage.getItem(LOADED_HASHES_KEY));
        expect(savedResult.length).toEqual(400);

        const firstPartOfResult = savedResult.slice(0, newHashes.length);
        expect(firstPartOfResult).toEqual(['4', '5', '6', '7']);
    });

    test('длина сохранённых хешей не выходит за предел, когда хранилище пустое', () => {
        const arr = getArrOfStrings(500);
        const accumulator = new HashesAccumulator();

        accumulator.add(arr);
        const savedInitial = JSON.parse(localStorage.getItem(LOADED_HASHES_KEY));
        expect(savedInitial.length).toEqual(400);
    });

    test('при выходе за квоту очищает хранилище и урезает лимит в два раза', () => {
        const accumulator = new HashesAccumulator();

        accumulator.add(['A', 'B', 'C', 'D']);
        expect(accumulator.getString()).toEqual('A,B,C,D');

        // из-за сложностей с моком localStorage и эмуляции переполнения
        // тестируем обработку ошибки напрямую
        // @ts-ignore
        accumulator.onError('', new DOMException('', 'QuotaExceededError'));

        expect(accumulator.getString()).toEqual('');
        // @ts-ignore
        expect(accumulator.hashesLimit).toEqual(200);
    });
});
