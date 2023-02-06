import {normalizeFilterDataForServer} from '../util';

describe('normalizeFilterDataForServer', () => {
    it('конвертируем value в array', () => {
        const filter = [
            {
                type: 'eq',
                value: 'joke',
            },
        ];

        const filterCopy = normalizeFilterDataForServer(filter);

        expect(Array.isArray(filterCopy[0].value)).toBeTruthy();
    });

    it('не конвертируем value в array, если value уже в array', () => {
        const filter = [
            {
                type: 'eq',
                value: ['joke'],
            },
        ];
        const filterCopy = normalizeFilterDataForServer(filter);

        expect(Array.isArray(filterCopy[0].value)).toBeTruthy();
        expect(filterCopy[0].value[0]).toEqual('joke');
    });
});
