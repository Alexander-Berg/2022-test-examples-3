import * as cases from '../productTypeCases';

describe('getProductTypeCases', () => {
    it('Должен вернуть корректные склонения', () => {
        expect(cases.getProductTypeCases('book')).toEqual({
            nominative: 'книга',
            accusative: 'книгу',
            prepositional: 'книге',
            genitive: 'книги',
        });
        expect(cases.getProductTypeCases('cluster')).toEqual({
            nominative: 'товар',
            accusative: 'товар',
            prepositional: 'товаре',
            genitive: 'товара',
        });
    });

    it('Должен вернуть значения default', () => {
        expect(cases.getProductTypeCases('model')).toEqual({
            nominative: '',
            accusative: '',
            prepositional: '',
            genitive: '',
        });
    });
});

describe('getProductTypeNominative', () => {
    it('Должен тип продукта в именительном падеже', () => {
        expect(cases.getProductTypeNominative('cluster')).toEqual('товар');
        expect(cases.getProductTypeNominative('book')).toEqual('книга');
        expect(cases.getProductTypeNominative('model')).toEqual('');
    });
});

describe('getProductTypeAccusative', () => {
    it('Должен тип продукта в винительном падеже', () => {
        expect(cases.getProductTypeAccusative('cluster')).toEqual('товар');
        expect(cases.getProductTypeAccusative('book')).toEqual('книгу');
        expect(cases.getProductTypeAccusative('model')).toEqual('');
    });
});

describe('getProductTypePrepositional', () => {
    it('Должен тип продукта в предложном падеже', () => {
        expect(cases.getProductTypePrepositional('cluster')).toEqual('товаре');
        expect(cases.getProductTypePrepositional('book')).toEqual('книге');
        expect(cases.getProductTypePrepositional('model')).toEqual('');
    });
});

describe('getProductTypeGenitive', () => {
    it('Должен тип продукта в родительном падеже', () => {
        expect(cases.getProductTypeGenitive('cluster')).toEqual('товара');
        expect(cases.getProductTypeGenitive('book')).toEqual('книги');
        expect(cases.getProductTypeGenitive('model')).toEqual('');
    });
});
