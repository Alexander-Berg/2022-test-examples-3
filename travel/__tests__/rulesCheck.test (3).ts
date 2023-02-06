import oneOf from '../oneOf';

describe('Select validation rules', () => {
    test('При валидном значении возвращает true', () => {
        expect(oneOf(['1', '2', '3'], '1')).toBeTruthy();
    });

    test('При нее валидном значении возвращает true', () => {
        expect(oneOf(['1', '2', '3'], 'undefined')).toBeFalsy();
    });
});
