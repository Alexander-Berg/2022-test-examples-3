import { InitialParams, parseParams, buildRetpath } from './url';

describe('getInitialParams()', () => {
    it('should return all available params', () => {
        expect(parseParams('http://localhost/?stationId=123&columnId=234&fuelId=a95&volume=400'))
            .toStrictEqual<InitialParams>({
                stationId: '123',
                columnId: '234',
                fuelId: 'a95',
                volume: '400'
            });

        expect(parseParams('http://localhost/?stationId=123&columnId=234&fuelId=a95'))
            .toStrictEqual<InitialParams>({
                stationId: '123',
                columnId: '234',
                fuelId: 'a95'
            });

        expect(parseParams('http://localhost/?stationId=1&columnId=2'))
            .toStrictEqual<InitialParams>({
                stationId: '1',
                columnId: '2'
            });

        expect(parseParams('http://localhost/?stationId=abc'))
            .toStrictEqual<InitialParams>({
                stationId: 'abc'
            });

        expect(parseParams('http://localhost?stationId=abc'))
            .toStrictEqual<InitialParams>({
                stationId: 'abc'
            });
    });

    it('should not return volume without column', () => {
        expect(parseParams('http://localhost/?stationId=abc&fuelId=a95&volume=400'))
            .toStrictEqual<InitialParams>({
                stationId: 'abc'
            });
    });

    it('should not return fuel without column', () => {
        expect(parseParams('http://localhost/?stationId=abc&fuelId=a95'))
            .toStrictEqual<InitialParams>({
                stationId: 'abc'
            });
    });

    it('should not return volume without fuel', () => {
        expect(parseParams('http://localhost/?stationId=abc&columnId=2&volume=400'))
            .toStrictEqual<InitialParams>({
                stationId: 'abc',
                columnId: '2'
            });
    });

    it('should return empty object with invalid station', () => {
        expect(parseParams('http://localhost/?station='))
            .toStrictEqual<InitialParams>({});
    });
});

describe('buildRetpath()', () => {
    it('should build url with all params', () => {
        const params = {
            stationId: '1',
            columnId: '2',
            fuelId: 'a95',
            volume: '400'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1&columnId=2&fuelId=a95&volume=400');
    });

    it('should build url with station, column and fuel', () => {
        const params = {
            stationId: '1',
            columnId: '2',
            fuelId: 'a95'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1&columnId=2&fuelId=a95');
    });

    it('should build url with station and column', () => {
        const params = {
            stationId: '1',
            columnId: '2'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1&columnId=2');
    });

    it('should build url with station', () => {
        const params = {
            stationId: '1'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1');
    });

    it('should build url without any params', () => {
        const params = {};

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/');
    });

    it('should build url only for station', () => {
        const params = {
            stationId: '1',
            columnId: undefined,
            fuelId: 'a95',
            volume: '400'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1');
    });

    it('should build url only for station and column', () => {
        const params = {
            stationId: '1',
            columnId: '2',
            fuelId: undefined,
            volume: '400'
        };

        expect(buildRetpath(params))
            .toStrictEqual<string>('http://localhost/?stationId=1&columnId=2');
    });
});
