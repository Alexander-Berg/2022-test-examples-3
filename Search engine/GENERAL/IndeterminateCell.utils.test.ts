import {SelectableColumn} from '../../../interfaces';

import {checkIndeterminateInArray} from './IndeterminateCell.utils';

describe('checkIndeterminateInArray', () => {
    test('has no indeterminate value if all values selected', () => {
        const data = [
            {selected: true},
            {selected: true},
            {selected: true},
        ] as SelectableColumn[];

        expect(checkIndeterminateInArray(data)).toMatchObject({
            indeterminate: false,
            hasSelectedValues: true,
        });
    });
    test('has indeterminate value if values mixed', () => {
        const data = [
            {selected: true},
            {selected: true},
            {selected: false},
        ] as SelectableColumn[];

        expect(checkIndeterminateInArray(data)).toMatchObject({
            indeterminate: true,
            hasSelectedValues: true,
        });
    });
    test(`has no indeterminate values if all values isn't selected`, () => {
        const data = [
            {selected: false},
            {selected: false},
            {selected: false},
        ] as SelectableColumn[];

        expect(checkIndeterminateInArray(data)).toMatchObject({
            indeterminate: false,
            hasSelectedValues: false,
        });
    });
});
