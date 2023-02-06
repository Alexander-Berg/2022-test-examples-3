export const NORMAL_RESPONSE = {
    type: 'EditProperties',
    did: 'changeStateForm',
    caption: 'решить',
    attributes: [
        {
            code: 'categories',
            presentation: 'tree',
            required: true,
        },
        {
            code: 'tags',
            presentation: 'default',
            required: false,
        },
        {
            code: '@comment',
            presentation: 'default',
            required: false,
            permissions: {
                '@internalComment:edit': false,
                '@internalComment:create': false,
                '@publicComment:create': false,
                '@ticketContactComment:create': false,
            },
        },
    ],
};

export const NORMAL_REQUIRED_ATTRIBUTES = [
    {
        code: 'categories',
        presentation: 'tree',
        required: true,
    },
];
