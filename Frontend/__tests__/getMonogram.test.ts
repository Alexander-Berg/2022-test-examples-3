import getMonogram from '../getMonogram';

describe('getMonogram()', () => {
    it('пустота', () => {
        expect(getMonogram('')).toEqual('');
    });

    it('одно слово', () => {
        expect(getMonogram('MSSNGR')).toEqual('M');
        expect(getMonogram('Я')).toEqual('Я');
        expect(getMonogram('(Дом)')).toEqual('Д');
    });

    it('имя и отчество', () => {
        expect(getMonogram('Иван Петров')).toEqual('ИП');
    });

    it('несколько слов, больше двух', () => {
        expect(getMonogram('Чат, про котиков')).toEqual('ЧК');
        expect(getMonogram('Messenger продуктовый чат')).toEqual('MЧ');
    });

    it('разделитель не пробел', () => {
        expect(getMonogram('MSSNGR_Android')).toEqual('MA');
    });

    it('e-mail', () => {
        expect(getMonogram('unknown@ya.ru')).toEqual('UR');
    });

    it('эмоджи', () => {
        expect(getMonogram('😀😁')).toEqual('😀😁');
        expect(getMonogram('😀     ')).toEqual('😀');
        expect(getMonogram('👪👪')).toEqual('👪👪');
    });
});
