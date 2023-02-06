import { formatUserName } from './formatUserName';

describe('formatUserName', () => {
    it('возвращает пустую строку при отсутствии ФИО', () => {
        expect(formatUserName({})).toBe('');

        expect(formatUserName()).toBe('');
    });

    it('возвращает пустую строку при отсутствии имени', () => {
        expect(formatUserName({ last: 'Константинопольский' })).toBe('');
    });

    it('возвращает имя при отсутствии фамилии', () => {
        expect(formatUserName({ first: 'Константин' })).toBe('Константин');

        expect(formatUserName({ first: 'Константин', last: '' })).toBe('Константин');
    });

    it('возвращает фамилию и имя, если все представлено', () => {
        expect(formatUserName({ first: 'Константин', last: 'Константинопольский' }))
            .toBe('Константин Константинопольский');
    });
});
