import toJS from './toJS';

const originalData = {
    snake_case: 111,
    'kebab-case': 222,
    camelCase: 333,
};

describe('toJS', () => {
    it('должен возвращать объект с полямя в camel case', () => {
        const result = toJS(originalData);

        expect(result.snakeCase).toBe(originalData.snake_case);
        expect(result.kebabCase).toBe(originalData['kebab-case']);
        expect(result.camelCase).toBe(originalData.camelCase);

        expect(Object.keys(result).length).toEqual(3);
    });
});
