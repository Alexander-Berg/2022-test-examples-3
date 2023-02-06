import {EParamType} from '../types/TParams';

import ParamsExtractor from './ParamsExtractor';

describe('ParamsExtractor', () => {
    let extractor: ParamsExtractor;

    beforeEach(() => {
        extractor = new ParamsExtractor();
    });

    test('simple', () => {
        const code = '"Привет"';

        expect(extractor.extract(code)).toEqual({});
    });

    test('variable', () => {
        const code = 'params.name';

        expect(extractor.extract(code)).toEqual({name: EParamType.STRING});
    });

    test('variable and text', () => {
        const code = '"Мое имя: "+params.name';

        expect(extractor.extract(code)).toEqual({name: EParamType.OTHER});
    });

    test('if', () => {
        const code =
            '"Заграничный паспорт"+(params.number?": "+params.number:"")';

        expect(extractor.extract(code)).toEqual({
            number: EParamType.OTHER,
        });
    });

    test('if/else', () => {
        const code = '(params.short?"чт":"четверг")';

        expect(extractor.extract(code)).toEqual({short: EParamType.OTHER});
    });

    test('plural', () => {
        const code =
            'params.nights+" "+(params.nights % 10 === 1 && params.nights % 100 !== 11 ? "ночь" :(params.nights % 10 > 1 && params.nights % 10 < 5 && (params.nights % 100 < 10 || params.nights % 100 > 20) ? "ночи" : "ночей"))';

        expect(extractor.extract(code)).toEqual({nights: EParamType.NUMBER});
    });

    test('complex case', () => {
        const code =
            'params.name+" ребёнок "+((params.age<1)?"до 1 года":params.age+" "+(params.age % 10 === 1 && params.age % 100 !== 11 ? "год" :(params.age % 10 > 1 && params.age % 10 < 5 && (params.age % 100 < 10 || params.age % 100 > 20) ? "года" : "лет")))';

        expect(extractor.extract(code)).toEqual({
            name: EParamType.OTHER,
            age: EParamType.NUMBER,
        });
    });
});
