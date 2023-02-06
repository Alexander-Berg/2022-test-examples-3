module.exports = {
    id: '/LeftMenu',
    type: 'array',
    items: {
        type: 'object',
        properties: {
            name: { type: 'string' },
            id: { type: 'number' },
            elem: { type: 'string' },
            isCurrent: { type: 'boolean' },
            url: { type: 'string' },
            content: { type: 'array' }
        },
        additionalProperties: false,
        required: ['name', 'id', 'elem']
    }
};
