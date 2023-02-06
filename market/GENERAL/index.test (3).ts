import {stringifyQueryParams} from '.';

describe('stringifyQueryParams', () => {
    it('проверка простых типов', () => {
        const queryParams = stringifyQueryParams({first: 1, second: 'cabinet', third: true}).toString();

        expect(queryParams).toEqual('first=1&second=cabinet&third=true');
    });

    it('проверка массивов', () => {
        const queryParams = stringifyQueryParams({first: [1, 'cabinet', true]}).toString();

        expect(queryParams).toEqual('first=1&first=cabinet&first=true');
    });

    it('пустой массив', () => {
        const queryParams = stringifyQueryParams({first: []}).toString();

        expect(queryParams).toEqual('');
    });

    it('сочетание простых типов и массивов', () => {
        const queryParams = stringifyQueryParams({first: [1, 'cabinet', true], second: 'cabinet'}).toString();

        expect(queryParams).toEqual('first=1&first=cabinet&first=true&second=cabinet');
    });

    it('игнорирование неподходящих типов', () => {
        const queryParams = stringifyQueryParams({
            first: [null, undefined, {x: 10}, (): void => undefined],
            second: null,
            third: {x: 10},
            fourth: () => undefined,
            fifth: undefined,
        }).toString();

        expect(queryParams).toEqual('');
    });
});
