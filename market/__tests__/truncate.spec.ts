import {truncate} from '..';

describe('truncate', () => {
    it('should throw exception if the second argument is not a string', () => {
        expect(() => truncate({}, null)).toThrow();
        expect(() => truncate({})(null)).toThrow();
    });

    it('should return new string', () => {
        expect(truncate({}, 'Lorem ipsum dolor sit amet, consectetur')).toEqual(expect.any(String));
        expect(truncate({})('Lorem ipsum dolor sit amet, consectetur')).toEqual(expect.any(String));

        expect(truncate({length: 10}, 'Lorem ipsum dolor sit amet, consectetur')).toEqual(expect.any(String));
        expect(truncate({length: 10})('Lorem ipsum dolor sit amet, consectetur')).toEqual(expect.any(String));
    });

    it('should return truncated string', () => {
        expect(truncate({}, 'Lorem ipsum dolor sit amet, consectetur')).toEqual('Lorem ipsum dolor sit amet,...');
        expect(truncate({})('Lorem ipsum dolor sit amet, consectetur')).toEqual('Lorem ipsum dolor sit amet,...');

        expect(truncate({length: 10, omission: '…'}, 'Lorem ipsum dolor sit amet, consectetur')).toEqual('Lorem ips…');
        expect(truncate({length: 10, omission: '…'})('Lorem ipsum dolor sit amet, consectetur')).toEqual('Lorem ips…');
    });

    it('should have right types', () => {
        /* eslint-disable no-unused-vars */
        /* eslint-disable no-unused-expressions */
        const curryResult = truncate({});
        (curryResult as (a: string) => string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (curryResult as (a: any) => string);

        const curryTruncateResult = truncate({})('Lorem ipsum dolor sit amet, consectetur');
        (curryTruncateResult as string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (curryTruncateResult as any);

        const truncateWithOptionsResult = truncate({length: 10})('Lorem ipsum dolor sit amet, consectetur');
        (truncateWithOptionsResult as string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (truncateWithOptionsResult as any);

        // @ts-expect-error
        const truncateWithWrongOptionsResult = truncate({smth: true})('Lorem ipsum dolor sit amet, consectetur');

        /* eslint-enable no-unused-vars */
        /* eslint-enable no-unused-expressions */
    });
});
