import deepFreeze from 'deep-freeze';
import { SET_URL, UPDATE_PATHNAME, ADD_QUERY_PARAMS, REMOVE_QUERY_PARAMS } from '../../../../../src/store/action-types';
import url from '../../../../../src/store/reducers/url';

describe('url reducer', () => {
    let defaultState;

    beforeEach(() => {
        defaultState = {};
        deepFreeze(defaultState);
    });

    it('состояние по умолчанию', () => {
        expect(url(undefined, {})).toEqual(defaultState);
    });

    it('SET_URL', () => {
        const urlObject = {
            pathname: '/i/short-hash',
            query: {
                pane: 'file-info'
            }
        };
        expect(
            url(defaultState, {
                type: SET_URL,
                url: urlObject
            })
        ).toEqual(urlObject);
    });

    it('SET_URL должен заменять пробел на + в хэше', () => {
        const state = url(defaultState, {
            type: SET_URL,
            url: {
                pathname: '/mail',
                query: {
                    hash: '8AkumSJkuq0Iih0R2j YAxNQL0NBxs4rW6w6tBHM4kE='
                }
            }
        });
        expect(state.query.hash).toEqual('8AkumSJkuq0Iih0R2j+YAxNQL0NBxs4rW6w6tBHM4kE=');
    });

    it('UPDATE_PATHNAME', () => {
        expect(
            url(defaultState, {
                type: UPDATE_PATHNAME,
                pathname: 'some pathname'
            })
        ).toEqual({ pathname: 'some pathname' });
    });

    it('ADD_QUERY_PARAMS', () => {
        expect(
            url(defaultState, {
                type: ADD_QUERY_PARAMS,
                params: { query_name: 'query_value' }
            })
        ).toEqual({ query: { query_name: 'query_value' } });
    });

    it('ADD_QUERY_PARAMS должен возвращать новые объекты если параметр поменялся', () => {
        const stateBefore = {
            pathname: '/d/aaa',
            query: {
                first: '1st',
                second: '2nd'
            }
        };
        deepFreeze(stateBefore);
        const stateAfter = url(stateBefore, {
            type: ADD_QUERY_PARAMS,
            params: { first: '3rd' }
        });
        expect(stateAfter).toEqual(Object.assign({}, stateBefore, {
            query: Object.assign({}, stateBefore.query, { first: '3rd' })
        }));
        expect(stateBefore === stateAfter).toEqual(false);
        expect(stateBefore.query === stateAfter.query).toEqual(false);
    });

    it('ADD_QUERY_PARAMS должен возвращать старый объект если параметр НЕ поменялся', () => {
        const stateBefore = {
            pathname: '/i/bbb',
            query: {
                first: '1st',
                second: '2nd'
            }
        };
        deepFreeze(stateBefore);
        const stateAfter = url(stateBefore, {
            type: ADD_QUERY_PARAMS,
            params: { first: '1st' }
        });
        expect(stateAfter).toEqual(stateBefore);
        expect(stateBefore === stateAfter).toEqual(true);
        expect(stateBefore.query === stateAfter.query).toEqual(true);
    });

    it('ADD_QUERY_PARAMS должен заменять пробел на + в хэше', () => {
        const stateBefore = {
            pathname: '/mail'
        };
        deepFreeze(stateBefore);
        const stateAfter = url(stateBefore, {
            type: ADD_QUERY_PARAMS,
            params: { hash: '8AkumSJkuq0Iih0R2j YAxNQL0NBxs4rW6w6tBHM4kE=' }
        });
        expect(stateAfter.query.hash).toEqual('8AkumSJkuq0Iih0R2j+YAxNQL0NBxs4rW6w6tBHM4kE=');
    });

    it('REMOVE_QUERY_PARAMS', () => {
        const stateBefore = {
            pathname: '/d/ccc',
            query: {
                first: '1st',
                second: '2nd'
            }
        };
        deepFreeze(stateBefore);

        expect(url(stateBefore, {
            type: REMOVE_QUERY_PARAMS,
            params: ['first']
        })).toEqual({
            pathname: '/d/ccc',
            query: {
                second: '2nd'
            }
        });
    });

    it('REMOVE_QUERY_PARAMS должен возвращать старый объект если удаляемого параметра нет', () => {
        const stateBefore = {
            pathname: '/i/eee',
            query: {
                first: '1st',
                second: '2nd'
            }
        };
        deepFreeze(stateBefore);
        const stateAfter = url(stateBefore, {
            type: REMOVE_QUERY_PARAMS,
            params: ['third']
        });
        expect(stateAfter).toEqual(stateBefore);
        expect(stateBefore === stateAfter).toEqual(true);
        expect(stateBefore.query === stateAfter.query).toEqual(true);
    });
});
