'use strict';

module.exports = [
    {
        name: 'affiliate-test:index',
        pattern: '/test',
        data: {
            method: 'GET',
            pageName: 'Test',
        },
    },
    {
        name: 'affiliate-test:widgets-gemini',
        pattern: '/test/widgets/gemini/<widgetType>',
        data: {
            method: 'GET',
            pageName: 'WidgetsGeminiPage',
        },
    },
    {
        name: 'affiliate-test:widgets-hermione',
        pattern: '/test/widgets/hermione',
        data: {
            method: 'GET',
            pageName: 'WidgetsHermionePage',
        },
    },
];
