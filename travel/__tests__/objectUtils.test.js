const {getObjectWithoutProps, bindMethods} =
    require.requireActual('../objectUtils');

const objectWithProps = {
    prop1: 1,
    prop2: 2,
    prop3: 3,
};
const propsToDeleting = ['prop1', 'prop2'];
const objectAfterDeletingProps = {
    prop3: 3,
};

const $this = {
    data: 'data',
    fn: jest.fn().mockReturnThis(),
};

describe('Функция getObjectWithoutProps', () => {
    it(`Должна удалять из объека свойства перечисленные в массиве
        и возвращать получившийся объект`, () => {
        const result = getObjectWithoutProps(objectWithProps, propsToDeleting);

        expect(result).toEqual(objectAfterDeletingProps);
    });
});

describe('Функция bindMethods', () => {
    it('Должна установить ключевое слово "this" в указанный объект', () => {
        bindMethods($this, ['fn']);
        const bindedFn = $this.fn;

        expect(bindedFn()).toBe($this);
    });
});
