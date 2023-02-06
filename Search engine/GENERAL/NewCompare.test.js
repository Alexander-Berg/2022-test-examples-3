import {
    serpsetIds,
    serpsetFilter,
    rawData,
    parsedData,
} from './NewCompare.test.data';
import {CompareGroups} from './NewCompare';

describe('New Compare: CompareGroups parse', () => {
    const compareGroups = new CompareGroups(null);
    compareGroups.filters.set({
        serpset: serpsetIds,
        serpsetFilter,
    });

    test('All', () => {
        const data = compareGroups.parse(rawData);
        const preparedData = parsedData;

        expect(data).toEqual(preparedData);
    });
});
