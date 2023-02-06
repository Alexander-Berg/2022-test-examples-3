import {prepareQaAttributes} from '../qaAttributes';

describe('prepareQaAttributes', () => {
    it('when pass string should return an object with the same string', () => {
        const str = 'aaa';

        expect(prepareQaAttributes(str)).toEqual({
            ['data-qa']: str,
        });
    });

    it('when pass object should return an object with compose string', () => {
        const obj = {
            key: 'key',
            parent: 'parent',
            current: 'current',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: 'key-parent-current',
        });
    });

    it('when pass object with key number should return an object with compose string', () => {
        const obj = {
            key: 1,
            parent: 'parent',
            current: 'current',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: '1-parent-current',
        });
    });

    it('when pass object without field should return an object with string which compose without this field', () => {
        const obj = {
            parent: 'parent',
            current: 'current',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: 'parent-current',
        });
    });

    it('when pass object with zero field should return an object with string which compose with "0"', () => {
        const obj = {
            key: 0,
            parent: 'parent',
            current: 'current',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: '0-parent-current',
        });
    });

    it('when pass object with nested parent field should return an object with all parent prefix values', () => {
        const obj = {
            key: 'key',
            parent: {
                parent: 'mainParent',
                key: 'parentKey',
            },
            current: 'current',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: 'key-parentKey-mainParent-current',
        });
    });

    it('when pass component props object with data-qa should return equal data-qa object', () => {
        const obj = {
            label: 'Hello',
            onClick: function (): void {},
            ['data-qa']: 'aaa',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: 'aaa',
        });
    });

    it('parent === undefined ', () => {
        const obj = {
            key: 1,
            parent: undefined,
            current: 'qwerty',
        };

        expect(prepareQaAttributes(obj)).toEqual({
            ['data-qa']: '1-qwerty',
        });
    });
});
