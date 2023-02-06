import {template} from '..';
import type {Options} from '../types/template';

describe('template', () => {
    it('should throw exception if the second argument is not a string', () => {
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => template({}, null)).toThrow();
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => template({})(null)).toThrow();
    });

    it('should return new function', () => {
        expect(template({}, 'Hello, <%= name %>!')).toEqual(expect.any(Function));
        expect(template({})('Hello, <%= name %>!')).toEqual(expect.any(Function));

        expect(template({interpolate: /{{([\s\S]+?)}}/g}, 'Hello, {{ name }}!')).toEqual(expect.any(Function));
        expect(template({interpolate: /{{([\s\S]+?)}}/g})('Hello, {{ name }}!')).toEqual(expect.any(Function));
    });

    it('should return string', () => {
        expect(template({}, 'Hello, <%= name %>!')({name: 'Mraket'})).toEqual('Hello, Mraket!');
        expect(template({})('Hello, <%= name %>!')({name: 'Mraket'})).toEqual('Hello, Mraket!');

        expect(template({interpolate: /{{([\s\S]+?)}}/g}, 'Hello, {{ name }}!')({name: 'Mraket'})).toEqual('Hello, Mraket!');
        expect(template({interpolate: /{{([\s\S]+?)}}/g})('Hello, {{ name }}!')({name: 'Mraket'})).toEqual('Hello, Mraket!');
    });

    it('should have right types', () => {
        /* eslint-disable no-unused-vars */
        /* eslint-disable no-unused-expressions */
        const curryResult = template({});
        (curryResult as (a: string) => (a: Options) => string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (curryResult as (a: string) => (a: any) => string);

        const curryTemplateResult = template({})('Hello, <%= name %>!');
        (curryTemplateResult as (a: Options) => string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (curryTemplateResult as (a: any) => string);

        const templateResult = template({}, 'Hello, <%= name %>!');
        (templateResult as (a: Options) => string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (templateResult as (a: any) => string);

        const result = templateResult({name: 'Mraket'});
        (result as string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (result as null);

        const templateWithSettingsResult = template({interpolate: /{{([\s\S]+?)}}/g})('Hello, {{ name }}!');
        (templateWithSettingsResult as (a: Options) => string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (templateWithSettingsResult as (a: any) => string);

        const anotherResult = templateWithSettingsResult({name: 'Mraket'});
        (anotherResult as string);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        (anotherResult as null);

        // @ts-expect-error
        const templateWithWrongSettingsResult = template({smth: true})('Hello, <%= name %>!');

        /* eslint-enable no-unused-vars */
        /* eslint-enable no-unused-expressions */
    });
});
