import mobilizer from './mobilize-domains';

describe('Domain data mobilizer', () => {
    const testTable = [
        {
            name: 'should not change input domain data',
            input: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                rules: ['shop', 'market-shop'],
            },
            expected: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                rules: ['shop', 'market-shop'],
            },
        },
        {
            name: 'should mobilize rules',
            input: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                rules: ['shop', 'market-shop', 'mobile-shop'],
            },
            expected: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                rules: ['shop'],
            },
        },
        {
            name: 'should mobilize selectors',
            input: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                'mobile-selector': {
                    price: '.mobile__price',
                    name: '.mobile__name',
                },
                rules: ['shop'],
            },
            expected: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.mobile__price',
                    name: '.mobile__name',
                },
                rules: ['shop'],
            },
        },
        {
            name: 'should mobilize selectors and rules',
            input: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.price',
                    name: '.name',
                },
                'mobile-selector': {
                    price: '.mobile__price',
                    name: '.mobile__name',
                },
                rules: ['shop', 'market-shop', 'mobile-shop'],
            },
            expected: {
                urlTemplates: [/^kek.com$/],
                selector: {
                    price: '.mobile__price',
                    name: '.mobile__name',
                },
                rules: ['shop'],
            },
        },
    ];

    testTable.forEach(({ name, input, expected }) => {
        test(name, () => {
            expect(mobilizer(input)).toEqual(expected);
        });
    });
});
