module.exports = {
    id: '/PageSections',
    type: 'array',
    items: {
        type: 'object',
        properties: {
            block: { type: 'string' },
            mix: {
                type: 'object',
                properties: {
                    block: { type: 'string' },
                    elem: { type: 'string' }
                }
            },
            data: {
                type: 'object',
                properties: {
                    title: { type: 'string' },
                    order: { type: 'number' },
                    enabled: { type: 'boolean' }
                }
            },
            productUrl: { type: 'string' },
            productsListUrl: { type: 'string' }
        },
        additionalProperties: false,
        required: ['block', 'mix', 'data', 'productUrl', 'productsListUrl']
    }
};
