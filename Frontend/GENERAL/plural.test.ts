import { plural } from './plural';

const forms = {
    none: 'нет статей',
    one: '{n} статья',
    some: '{n} статьи',
    many: '{n} статей'
};

test('`plural` правильно выбирает форму `none`', () => {
    expect(plural(0, forms)).toEqual('нет статей');
});

test('`plural` правильно выбирает форму `one`', () => {
    expect(plural(1, forms)).toEqual('{n} статья');
    expect(plural(21, forms)).toEqual('{n} статья');
    expect(plural(121, forms)).toEqual('{n} статья');
});

test('`plural` правильно выбирает форму `some`', () => {
    expect(plural(2, forms)).toEqual('{n} статьи');
    expect(plural(23, forms)).toEqual('{n} статьи');
    expect(plural(224, forms)).toEqual('{n} статьи');
});

test('`plural` правильно выбирает форму `many`', () => {
    expect(plural(10, forms)).toEqual('{n} статей');
    expect(plural(11, forms)).toEqual('{n} статей');
    expect(plural(12, forms)).toEqual('{n} статей');
    expect(plural(15, forms)).toEqual('{n} статей');

    expect(plural(110, forms)).toEqual('{n} статей');
    expect(plural(111, forms)).toEqual('{n} статей');
    expect(plural(112, forms)).toEqual('{n} статей');
    expect(plural(115, forms)).toEqual('{n} статей');

    expect(plural(5, forms)).toEqual('{n} статей');
    expect(plural(6, forms)).toEqual('{n} статей');
    expect(plural(7, forms)).toEqual('{n} статей');
    expect(plural(8, forms)).toEqual('{n} статей');
    expect(plural(9, forms)).toEqual('{n} статей');
    expect(plural(20, forms)).toEqual('{n} статей');

    expect(plural(105, forms)).toEqual('{n} статей');
    expect(plural(106, forms)).toEqual('{n} статей');
    expect(plural(107, forms)).toEqual('{n} статей');
    expect(plural(108, forms)).toEqual('{n} статей');
    expect(plural(109, forms)).toEqual('{n} статей');
    expect(plural(120, forms)).toEqual('{n} статей');
});
