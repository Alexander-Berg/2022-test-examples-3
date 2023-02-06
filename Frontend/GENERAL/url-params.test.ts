import { InitialParams, parseParams } from './url-params';

describe('Lib. url-params.', () => {
    const defaultResult = {
        from: undefined,
        to: undefined,
        center: undefined,
        zoom: undefined,
        tariff: undefined,
        comment: undefined,
        clid: undefined,
        ref: undefined,
        requirements: undefined,
        promocode: undefined,
        utmContent: undefined,
        coupon: '',
        lang: undefined,
        map: undefined,
        promotionId: undefined,
        withLaunch: false,
        isCallcenterVisible: false,
        hideFullscreenPromotions: false,
        hidePromoblocks: false
    };

    describe('#parseParams', () => {
        it('should parse available params', () => {
            expect(parseParams('?from_point=30.0,20.0&to_point=20.0,30.0')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.0, 30.0],
                to: [30.0, 20.0],
            });

            expect(parseParams('?from_point=-30.0,20.0&to_point=20.0,-30.0')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.0, -30.0],
                to: [-30.0, 20.0],
            });

            expect(parseParams('?gfrom=30.0,20.0&gto=20.0,30.0')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.0, 30.0],
                to: [30.0, 20.0],
            });

            expect(parseParams('?gfrom=-30.0,20.0&gto=20.0,-30.0')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.0, -30.0],
                to: [-30.0, 20.0],
            });

            expect(parseParams('?from_point=30.123,20.456&to_point=20.123,-30.456')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.456, 30.123],
                to: [-30.456, 20.123],
            });

            expect(parseParams('?from_point=30.0,20.0')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [20.0, 30.0],
            });

            expect(parseParams('?ll=30.0,20.0&zoom=15')).toStrictEqual<InitialParams>({
                ...defaultResult,
                center: [20.0, 30.0],
                zoom: 15,
            });

            expect(parseParams('?clid=abcd12&ref=test')).toStrictEqual<InitialParams>({
                ...defaultResult,
                clid: 'abcd12',
                ref: 'test',
            });

            expect(parseParams('?requirements=animals=1;nosmoking=true;childchair=1,1,ski=false')).toStrictEqual<
                InitialParams
            >({
                ...defaultResult,
                requirements: {
                    animals: 1,
                    nosmoking: true,
                    childchair: [1, 1],
                },
            });
            expect(parseParams('?map=g')).toStrictEqual<InitialParams>({
                ...defaultResult,
                map: 'g',
            });
        });

        it('should not return invalid params', () => {
            expect(parseParams('?from_point=abc&to_point=def')).toStrictEqual<InitialParams>({
                ...defaultResult,
            });

            expect(parseParams('?from_point=20.0,30.0&to_point=def')).toStrictEqual<InitialParams>({
                ...defaultResult,
                from: [30.0, 20.0],
            });

            expect(parseParams('?from_point=20.0,abc&to_point=def')).toStrictEqual<InitialParams>({
                ...defaultResult,
            });

            expect(parseParams('?zoom=qwe')).toStrictEqual<InitialParams>({
                ...defaultResult,
            });
        });
    });
});
