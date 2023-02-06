jest.disableAutomock();

import {useSuburbanSearchContext} from '../../contextUtils';

const context = {
    from: {
        key: '',
        title: '',
        slug: '',
    },
    to: {
        key: '',
        title: '',
        slug: '',
    },
    searchNext: true,
};

describe('useSuburbanSearchContext', () => {
    it('Надо использовать пригородный поисковый контекст', () => {
        expect(useSuburbanSearchContext(context)).toBe(true);
    });

    it('Не надо использовать пригородный поисковый контекст', () => {
        expect(
            useSuburbanSearchContext({
                ...context,
                searchNext: false,
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                from: {
                    ...context.from,
                    key: 'c54',
                },
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                from: {
                    ...context.from,
                    title: 'Екатеринбург',
                },
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                from: {
                    ...context.from,
                    slug: 'yekaterunburg',
                },
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                to: {
                    ...context.to,
                    key: 'c54',
                },
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                to: {
                    ...context.to,
                    title: 'Екатеринбург',
                },
            }),
        ).toBe(false);

        expect(
            useSuburbanSearchContext({
                ...context,
                to: {
                    ...context.to,
                    slug: 'yekaterunburg',
                },
            }),
        ).toBe(false);
    });
});
