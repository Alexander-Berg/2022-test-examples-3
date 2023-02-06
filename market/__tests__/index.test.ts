/**
 * @jest-environment jsdom
 */

import {renderHook, act} from '@testing-library/react-hooks';

import {entitiesHooks} from 'entities';

import usePostProcessing, {
    NOT_STARTED_YET_ERROR,
    ALREADY_STARTED_ERROR,
    POST_PROCESSING_NORMAL_STAGE,
    POST_PROCESSING_EXTRA_STAGE,
} from '..';

jest.mock('entities', () => ({
    entitiesHooks: {
        useGid: jest.fn(),
        useEntityCard: jest.fn(),
        useCurrentEntity: jest.fn(),
    },
}));

describe('usePostProcessing hook', () => {
    const STAB_TIME = new Date('2020-06-08T00:10:00Z').getTime();
    const RealDateNow = Date.now;

    beforeAll(() => {
        global.Date.now = jest.fn(() => STAB_TIME);
    });

    afterAll(() => {
        global.Date.now = RealDateNow;
    });

    it('returns correct data without any contexts', () => {
        const {result} = renderHook(usePostProcessing);

        expect(result.current.duration).toBeNull();
        expect(result.current.extraTimeRequested).toBeFalsy();
        expect(result.current.ticketCategoryForMissed).toBeNull();
        expect(typeof result.current.start).toBe('function');
        expect(typeof result.current.requestExtraTime).toBe('function');
    });

    it('cannot add extra time until start timer', () => {
        const {result} = renderHook(usePostProcessing);

        expect(result.current.requestExtraTime).toThrow(NOT_STARTED_YET_ERROR);
    });

    it('starts timer once', () => {
        const {result} = renderHook(usePostProcessing);

        expect(result.current.duration).toBeNull();

        act(() => {
            result.current.start();
        });

        expect(result.current.duration).toBe(POST_PROCESSING_NORMAL_STAGE);
        expect(result.current.start).toThrow(ALREADY_STARTED_ERROR);
    });

    test('request extra time changes appropriate flag and duration', () => {
        const {result} = renderHook(usePostProcessing);

        act(() => {
            result.current.start();
        });

        expect(result.current.extraTimeRequested).toBeFalsy();
        expect(result.current.duration).toBe(POST_PROCESSING_NORMAL_STAGE);

        act(() => {
            result.current.requestExtraTime();
        });

        expect(result.current.extraTimeRequested).toBeTruthy();
        expect(result.current.duration).toBe(POST_PROCESSING_NORMAL_STAGE + POST_PROCESSING_EXTRA_STAGE);
    });

    it('works with custom values from context', () => {
        (entitiesHooks.useEntityCard as jest.Mock).mockReturnValue({
            extensions: {
                ticket: {
                    postProcessing: 3000,
                    additionalPostProcessing: 3000,
                },
            },
        });

        const {result} = renderHook(usePostProcessing);

        act(() => {
            result.current.start();
        });

        expect(result.current.extraTimeRequested).toBeFalsy();
        expect(result.current.duration).toBe(3000);

        act(() => {
            result.current.requestExtraTime();
        });

        expect(result.current.extraTimeRequested).toBeTruthy();
        expect(result.current.duration).toBe(6000);
    });
});
