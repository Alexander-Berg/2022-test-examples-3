import { validateBBTags } from './index';

describe('validateBBTags testing', () => {
    it('Empty string', () => {
        const TEST_STR = '';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('String without tags', () => {
        const TEST_STR = 'aaaaaa';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-1', () => {
        const TEST_STR = '[b]bold[/b]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-2', () => {
        const TEST_STR = '[color=#FFF]bold[/color]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-3', () => {
        const TEST_STR = '[color=#FFF][b]bold[/color][/b]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-4', () => {
        const TEST_STR = 'some text [br]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-5', () => {
        const TEST_STR = '[line-height=16px]some text [/line-height]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Valid case-6', () => {
        const TEST_STR = 'text [url=\'someLink\']link[/url]';
        expect(validateBBTags(TEST_STR)).toBeTruthy();
    });

    it('Invalid case-1', () => {
        const TEST_STR = '[]';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-2', () => {
        const TEST_STR = ']b[';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-3', () => {
        const TEST_STR = '[bbb]';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-4', () => {
        const TEST_STR = '[\'ticket#21042806533299560\']';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-5', () => {
        const TEST_STR = '↵🚧|❌][__🚗__][__🚙__]↵';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-6', () => {
        const TEST_STR = '[b]bold';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-7', () => {
        const TEST_STR = '[b]bold[/b';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-8', () => {
        const TEST_STR = '[b bold[/b]';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-9', () => {
        const TEST_STR = 'bold[/b]';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });

    it('Invalid case-10', () => {
        const TEST_STR = '[/b][b][i][/i][/b]';
        expect(validateBBTags(TEST_STR)).toBeFalsy();
    });
});
