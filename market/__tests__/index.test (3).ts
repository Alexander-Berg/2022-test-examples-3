import {renderHook} from '@testing-library/react-hooks';

import {useGetRequest} from 'common/hooks/useRequest';

import {NORMAL_RESPONSE, NORMAL_REQUIRED_ATTRIBUTES} from './fixtures';
import useTransitionAttributes from '..';

jest.mock('react-redux', () => ({
    ...jest.requireActual('react-redux'),
    useSelector: jest.fn(),
}));

jest.mock('common/hooks/useRequest', () => ({
    useGetRequest: jest.fn(),
}));

jest.mock('entities', () => ({
    entitiesHooks: {
        useCurrentEntity: jest.fn(),
        useCurrentEntityLoading: jest.fn(),
        useCurrentEntityChanges: jest.fn(),
    },
}));

describe('useTransitionAttributes hook', () => {
    it('just works', () => {
        (useGetRequest as jest.Mock).mockImplementation(() => ({
            result: null,
            execRequest: jest.fn(),
        }));
        const {result} = renderHook(useTransitionAttributes);

        expect(result.current.commentAttribute).toBeNull();
        expect(result.current.loading).toBeFalsy();
        expect(result.current.requiredAttributes).toHaveLength(0);
        expect(result.current.requiredAttributesIsFilled).toBe(true);
    });

    it('try to fetch transition form', () => {
        const execRequestMock = jest.fn();

        (useGetRequest as jest.Mock).mockImplementation(() => ({
            result: null,
            execRequest: execRequestMock,
        }));

        renderHook(useTransitionAttributes);

        expect(execRequestMock).toHaveBeenCalledTimes(1);
    });

    it('transforms the transtion attributes', () => {
        const execRequestMock = jest.fn();

        (useGetRequest as jest.Mock).mockImplementation(() => ({
            result: NORMAL_RESPONSE,
            execRequest: execRequestMock,
        }));

        const {result} = renderHook(useTransitionAttributes);

        expect(result.current.requiredAttributes).toEqual(NORMAL_REQUIRED_ATTRIBUTES);
    });
});
