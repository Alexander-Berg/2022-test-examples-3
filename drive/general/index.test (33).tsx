import { isValidSessionId } from './index';

const VALID_SESSION_ID = '6a2b8fe4-52057-f1edc472-9de684b3';
const INVALID_SESSION_ID = 'qwertyui-52057e48-f1edc472-9de684b3';
const INVALID_SESSION_ID_1 = '6a2b8fe7-52057e48?ce87fa01=f1edc584';

describe('Session Id validation', () => {
    it('valid session id', () => {
        expect(isValidSessionId(VALID_SESSION_ID)).toBe(true);
    });

    it('invalid session id', () => {
        expect(isValidSessionId(INVALID_SESSION_ID)).toBe(false);
    });

    it('invalid session id1', () => {
        expect(isValidSessionId(INVALID_SESSION_ID_1)).toBe(false);
    });

    it('empty string session id', () => {
        expect(isValidSessionId('')).toBe(false);
    });

    it('empty session id', () => {
        expect(isValidSessionId(null)).toBe(false);
    });

    it('undefined session id', () => {
        expect(isValidSessionId(undefined)).toBe(false);
    });
});
