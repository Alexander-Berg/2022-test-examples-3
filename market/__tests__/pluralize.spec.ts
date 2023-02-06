import pluralize from '../pluralize';

declare const it: jest.It;

const dayForms = ['день', 'дня', 'дней'];
const dayForms0 = ['день', 'дня', 'дней', 'дней'];

const formsWithPlaceholder = ['Выбран %d объект', 'Выбрано %d объекта', 'Выбрано %d объектов'];

const formsWithPlaceholder0 = ['Выбран %d объект', 'Выбрано %d объекта', 'Выбрано %d объектов', 'Объекты не выбраны'];

describe('pluralize', () => {
    it.each`
        number  | forms                    | expectedForm
        ${0}    | ${dayForms0}             | ${'дней'}
        ${0}    | ${dayForms}              | ${'дней'}
        ${1}    | ${dayForms}              | ${'день'}
        ${2}    | ${dayForms}              | ${'дня'}
        ${6}    | ${dayForms}              | ${'дней'}
        ${11}   | ${dayForms}              | ${'дней'}
        ${12}   | ${dayForms}              | ${'дней'}
        ${14}   | ${dayForms}              | ${'дней'}
        ${21}   | ${dayForms}              | ${'день'}
        ${22}   | ${dayForms}              | ${'дня'}
        ${3333} | ${dayForms}              | ${'дня'}
        ${0}    | ${formsWithPlaceholder0} | ${'Объекты не выбраны'}
        ${0}    | ${formsWithPlaceholder}  | ${'Выбрано 0 объектов'}
        ${1}    | ${formsWithPlaceholder}  | ${'Выбран 1 объект'}
        ${2}    | ${formsWithPlaceholder}  | ${'Выбрано 2 объекта'}
        ${6}    | ${formsWithPlaceholder}  | ${'Выбрано 6 объектов'}
        ${11}   | ${formsWithPlaceholder}  | ${'Выбрано 11 объектов'}
        ${12}   | ${formsWithPlaceholder}  | ${'Выбрано 12 объектов'}
        ${14}   | ${formsWithPlaceholder}  | ${'Выбрано 14 объектов'}
        ${21}   | ${formsWithPlaceholder}  | ${'Выбран 21 объект'}
        ${22}   | ${formsWithPlaceholder}  | ${'Выбрано 22 объекта'}
        ${3333} | ${formsWithPlaceholder}  | ${'Выбрано 3333 объекта'}
    `('$number $expectedForm', ({number, forms, expectedForm}) => {
        expect(pluralize(number, forms)).toBe(expectedForm);
    });
});
