import BodyExtractor from './BodyExtractor';

describe('BodyExtractor', () => {
    let extractor: BodyExtractor;

    beforeEach(() => {
        extractor = new BodyExtractor();
    });

    test('without params', () => {
        const code = 'function(){return "Привет";}';

        expect(extractor.extract(code)).toBe('"Привет"');
    });

    test('with params', () => {
        const code = 'function(params){return "Мое имя: "+params.name;}';

        expect(extractor.extract(code)).toBe('"Мое имя: "+params.name');
    });
});
