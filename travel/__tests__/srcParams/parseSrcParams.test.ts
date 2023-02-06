import {parseSrcParams} from 'server/utilities/srcParams/parseSrcParams';

describe('parseSrcParams', () => {
    it('srcParams не определён - вернёт пустой словарик', () => {
        expect(parseSrcParams({})).toEqual({});
    });

    it('Вернёт заполненный словарь для случая, когда srcParams - строка с одним параметром', () => {
        expect(parseSrcParams({srcParams: 'trains:flag1=value1'})).toEqual({
            trains: {
                flag1: 'value1',
            },
        });
    });

    it('srcParams содержит неизвестный entryPoint - вернёт словарик без неизвестного entryPoint', () => {
        expect(
            parseSrcParams({
                srcParams: 'trains:flag1=value1,metro:flag1=value1',
            }),
        ).toEqual({
            trains: {
                flag1: 'value1',
            },
        });
    });

    it('Вернёт заполненный словарь для случая, когда srcParams - строка с несколькими параметрами', () => {
        expect(
            parseSrcParams({
                srcParams:
                    'trains:flag1=value1,trains:flag2=value2,buses:flag1=value1',
            }),
        ).toEqual({
            trains: {
                flag1: 'value1',
                flag2: 'value2',
            },
            buses: {
                flag1: 'value1',
            },
        });
    });

    it('Вернёт заполненный словарь для случая, когда srcParams - массив (возьмет первый элемент)', () => {
        expect(
            parseSrcParams({
                srcParams: ['trains:flag1=value1', 'avia:flag1=value1'],
            }),
        ).toEqual({
            trains: {
                flag1: 'value1',
            },
        });
    });
});
