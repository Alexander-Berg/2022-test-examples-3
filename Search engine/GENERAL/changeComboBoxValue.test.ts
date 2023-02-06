import {changeComboBoxValue} from './changeComboBoxValue';

describe('changeComboBoxValue', () => {
    test('returns array if works with array value', () => {
        expect(changeComboBoxValue([], 'value')).toEqual(['value']);
        expect(changeComboBoxValue([], null)).toEqual([]);
        expect(changeComboBoxValue(['someValue', 'someValue1'], null)).toEqual(
            [],
        );
        expect(changeComboBoxValue(['someValue'], 'someValue')).toEqual([
            'someValue',
        ]);
        expect(
            changeComboBoxValue(['someValue', 'someValue1'], 'someValue'),
        ).toEqual(['someValue']);
        expect(
            changeComboBoxValue(
                ['someValue', 'someValue1'],
                'someValue2',
                false,
            ),
        ).toEqual(['someValue2']);
    });
    test('returns primitive value if works with primitives', () => {
        expect(changeComboBoxValue('word', 'newWord')).toEqual('newWord');
        expect(changeComboBoxValue('', 'word')).toEqual('word');
        expect(changeComboBoxValue('', null)).toBeNull();
        expect(changeComboBoxValue('word', null)).toBeNull();
        expect(changeComboBoxValue(2, 10)).toEqual(10);
    });
    test('passed ctrlOrCmd', () => {
        expect(
            changeComboBoxValue(
                ['someValue', 'someValue1'],
                'someValue2',
                true,
            ),
        ).toEqual(['someValue', 'someValue1', 'someValue2']);
        expect(changeComboBoxValue(['someValue2'], 'someValue2', true)).toEqual(
            [],
        );
        expect(
            changeComboBoxValue(
                ['someValue', 'someValue2'],
                'someValue2',
                true,
            ),
        ).toEqual(['someValue']);
    });
});
