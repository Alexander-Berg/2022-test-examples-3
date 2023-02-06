const {parseRawValue} = require('../../experiments');

describe('experiments', () => {
    describe('parseRawValue', () => {
        it('should coerce a value using a given type and return it if it occurs among experiment values', () => {
            const rawValue = '1000';

            const experiment = {
                type: Number,
                values: [
                    {value: 1000, percentage: 10},
                    {value: 2000, percentage: 20},
                ],
                defaultValue: 0,
            };

            expect(parseRawValue(rawValue, experiment)).toBe(1000);
        });

        it('should return defaultValue if a given value does not occur among experiment values', () => {
            const rawValue = '500';

            const experiment = {
                type: Number,
                values: [
                    {value: 1000, percentage: 10},
                    {value: 2000, percentage: 20},
                ],
                defaultValue: 0,
            };

            expect(parseRawValue(rawValue, experiment)).toBe(0);
        });
    });
});
