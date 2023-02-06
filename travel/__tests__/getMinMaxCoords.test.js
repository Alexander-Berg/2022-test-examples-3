jest.disableAutomock();

import getMinMaxCoords from '../getMinMaxCoords';

const normalCoord1 = [1, 20];
const normalCoord2 = [0, 25];
const normalCoord3 = ['2', '15'];

const oneNumberCoord = [4];
const nonNumbersCoord = ['what', 'test'];
const nonArrayCoord = 2019;
const nullCoord = [null, null];
const undefinedCoord = [undefined, undefined];
const emptyArrayCoord = [[], []];
const emptyStringCoord = ['', ''];
const tripleNumberCoord = [4, 5, 6];

describe('getMinMaxCoords', () => {
    it('Возвращает ожидаемый ответ для нормальных координат', () => {
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, normalCoord3]),
        ).toEqual([
            [0, 15],
            [2, 25],
        ]);
    });

    it('Возвращает ожидаемый ответ для нормальных координат, игнорируя координату в неправильном формате', () => {
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, oneNumberCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, nonNumbersCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, nonArrayCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, nullCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, undefinedCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, emptyArrayCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, emptyStringCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
        expect(
            getMinMaxCoords([normalCoord1, normalCoord2, tripleNumberCoord]),
        ).toEqual([
            [0, 20],
            [1, 25],
        ]);
    });

    it('Возвращает null если входной массив координат не массив, в нем не содержится корректных координат или этот массив пуст', () => {
        expect(getMinMaxCoords([])).toBeNull();
        expect(getMinMaxCoords(null)).toBeNull();
        expect(getMinMaxCoords(undefined)).toBeNull();
        expect(getMinMaxCoords(42)).toBeNull();
        expect(getMinMaxCoords('wrong')).toBeNull();
        expect(getMinMaxCoords({})).toBeNull();
        expect(
            getMinMaxCoords(oneNumberCoord, undefinedCoord, nullCoord),
        ).toBeNull();
    });
});
