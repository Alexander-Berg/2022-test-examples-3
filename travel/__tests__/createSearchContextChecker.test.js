import {InterruptError} from '../errors/executionErrors';
import createSearchContextChecker from '../createSearchContextChecker';
import compareSearchContexts from '../compareSearchContexts';

jest.mock('../compareSearchContexts');

let state;
const defaultContext = {foo: 'bar'};
const getState = () => state;

describe('createSearchContextChecker', () => {
    beforeEach(() => {
        state = {search: {context: defaultContext}};
    });

    it(`should return a function, that returns a promise resolved with a given value,
        if getState().search.context has not been changed`, () => {
        compareSearchContexts.mockReturnValue(true);

        const contextChecker = createSearchContextChecker(getState);

        return contextChecker('resolveValue').then(value => {
            expect(value).toBe('resolveValue');
            expect(compareSearchContexts).toBeCalledWith(
                defaultContext,
                defaultContext,
            );
        });
    });

    it(`should return a function, that returns a promise rejected with an error,
        if getState().search.context has been changed`, () => {
        compareSearchContexts.mockReturnValue(false);

        const contextChecker = createSearchContextChecker(getState);

        const context = {bar: 'foo'};

        state = {search: {context}};

        return contextChecker('resolveValue').catch(err => {
            expect(err instanceof InterruptError).toBe(true);
            expect(compareSearchContexts).toBeCalledWith(
                defaultContext,
                context,
            );
        });
    });
});
