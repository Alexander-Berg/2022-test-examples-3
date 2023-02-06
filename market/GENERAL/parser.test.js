import {parser} from './parser';

const UID = '9007199254740991';
const DEFAULT_BASE64 = 'H////////yc=';

describe('Parser', () => {
    it.each([
        [DEFAULT_BASE64, {isAuth: true, uid: UID, day: 7}],
        ['dcddCBY=', {isAuth: false, uid: '123499992', day: 22}],
    ])('Возвращает правильный результат', (str, result) => {
        const res = parser(str);
        expect(res).toStrictEqual(result);
    });
});
