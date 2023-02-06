import {ITrainsSimpleFilterOption} from 'types/trains/search/filters/ITrainsFilters';

import stationTo from '../../stationTo';

const options = [{value: '1'}, {value: '2'}] as ITrainsSimpleFilterOption[];

describe('stationTo', () => {
    describe('validateValue', () => {
        it('should return the same value - value contains only valid ids', () => {
            const value = ['1', '2'];

            expect(stationTo.validateValue(value, options)).toEqual(value);
        });

        it('should filter value - value contains unvalid id', () => {
            const value = ['1', '2', '3'];

            expect(stationTo.validateValue(value, options)).toEqual(['1', '2']);
        });

        it('should return empty value - value contains only unvalid ids', () => {
            const value = ['3'];

            expect(stationTo.validateValue(value, options)).toEqual([]);
        });
    });
});
