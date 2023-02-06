import validate from '../validate';
import {validateField} from '../utils';

jest.mock('../utils', () => ({
    ...require.requireActual('../utils'),
    validateField: jest.fn(),
}));

describe('searchHistory/validate', () => {
    describe('validate', () => {
        it('invalid from', () => {
            const from = {key: 1};
            const to = {key: 2};

            validateField.mockImplementation(field => field === to);
            expect(validate({from, to})).toBe(false);

            expect(validateField).toBeCalledWith(from);
        });

        it('invalid to', () => {
            const from = {key: 1};
            const to = {key: 2};

            validateField.mockImplementation(field => field === from);
            expect(validate({from, to})).toBe(false);

            expect(validateField).toBeCalledWith(to);
        });

        it('from.id == to.id', () => {
            const from = {key: 1, title: 'abc'};
            const to = {key: 1, title: 'qwerty'};

            validateField.mockImplementation(() => true);

            expect(validate({from, to})).toBe(false);

            expect(validateField).toBeCalledWith(from);
            expect(validateField).toBeCalledWith(to);
        });

        it('valid', () => {
            const from = {key: 1};
            const to = {key: 2};

            validateField.mockImplementation(() => true);

            expect(validate({from, to})).toBe(true);

            expect(validateField).toBeCalledWith(from);
            expect(validateField).toBeCalledWith(to);
        });
    });
});
