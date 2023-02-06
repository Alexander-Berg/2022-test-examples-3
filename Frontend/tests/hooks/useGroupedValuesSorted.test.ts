import type { IFilters } from '@src/typings/filters';
import { useGroupedValuesSorted } from '../../hooks/useGroupedValuesSorted';

describe('Filters/hooks/useGroupedValuesSorted', () => {
    it('Возвращает сортированный список enum значений', () => {
        const appliedFilters: IFilters = {
            size: {
                type: 'enum',
                title: '',
                id: 'size',
                param: '',
                values: [
                    { value: '1', found: 0, id: '1' },
                    { value: '2', found: 1, id: '2' },
                    { value: '3', found: 1, id: '3', checked: true },
                ],
            },
        };
        const groupedValues = [{
            groupName: '1',
            values: [
                { value: '1', found: 0, id: '1' },
                { value: '2', found: 1, id: '2' },
                { value: '3', found: 1, id: '3', checked: true },
            ]
        }];
        const sortedGroupedValues = useGroupedValuesSorted('size', groupedValues, appliedFilters);

        expect(sortedGroupedValues).toStrictEqual([{
            groupName: '1',
            values: [
                { value: '3', found: 1, id: '3', checked: true },
                { value: '2', found: 1, id: '2' },
                { value: '1', found: 0, id: '1' },
            ]
        }]);
    });
});
