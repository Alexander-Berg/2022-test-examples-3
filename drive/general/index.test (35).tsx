import { EVENT_TYPES } from '../eventTypes/eventTypes';
import { validateParams } from './index';

const HAS_REQUIRED_PARAMS_ONLY = {
    a: 'test_test',
    b: undefined,
};

const HAS_OPTIONAL_PARAMS_ONLY = {
    c: 'test_test',
    d: undefined,
};
const HAS_MIXED_PARAMS = {
    a: 'test_test',
    b: undefined,
    c: 123,
    d: {},
    e: { a: 1 },
};

describe('Validate', () => {
    it('has only event_type and empty event_type_details', () => {
        expect(validateParams({
            event_type: EVENT_TYPES.TEST_EMPTY,
        })).toBe(true);
    });

    it('has only event_type and mixed event_type_details', () => {
        expect(validateParams({
            event_type: EVENT_TYPES.TEST_MIXED,
        })).toBe(false);
    });

    it('has required params only and mixed event_type_details; has all required params', () => {
        expect(validateParams({
            ...HAS_REQUIRED_PARAMS_ONLY,
            event_type: EVENT_TYPES.TEST_MIXED,
        })).toBe(true);
    });

    it('has unrequired params only and mixed event_type_details; doesn\'t have all unrequired params', () => {
        expect(validateParams({
            ...HAS_OPTIONAL_PARAMS_ONLY,
            event_type: EVENT_TYPES.TEST_MIXED,
        })).toBe(false);
    });

    it('has mixed params and mixed event_type_details: doesn\'t have unlisted params', () => {
        expect(validateParams({
            ...HAS_MIXED_PARAMS,
            event_type: EVENT_TYPES.TEST_MIXED,
        })).toBe(true);
    });

    it('has mixed params and mixed event_type_details: has unlisted params', () => {
        expect(validateParams({
            ...HAS_MIXED_PARAMS,
            f: 'a',
            event_type: EVENT_TYPES.TEST_MIXED,
        })).toBe(false);
    });

    it('has required params only and mixed event_type_details; has all required params and some unlisted params',
        () => {
            expect(validateParams({
                ...HAS_REQUIRED_PARAMS_ONLY,
                f: 'a',
                event_type: EVENT_TYPES.TEST_MIXED,
            })).toBe(false);
        });

    it('has mixed params and empty event_type_details', () => {
        expect(validateParams({
            ...HAS_MIXED_PARAMS,
            event_type: EVENT_TYPES.TEST_EMPTY,
        })).toBe(false);
    });

    it('has optional params only and optional event_type_details; has no unlisted params', () => {
        expect(validateParams({
            ...HAS_OPTIONAL_PARAMS_ONLY,
            event_type: EVENT_TYPES.TEST_UNREQUIRED,
        })).toBe(true);
    });
});
