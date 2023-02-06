module.exports = {
    id: '/Media',
    type: 'object',
    properties: {
        stories: { type: 'array' },
        cases: { type: 'array' },
        courses: { type: 'array' }
    },
    additionalProperties: false
};
