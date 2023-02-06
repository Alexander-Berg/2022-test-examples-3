module.exports = {
    id: '/PageModel',
    type: 'object',
    properties: {
        title: { type: 'string' },
        sectionTitle: { type: 'string' },
        image: { type: 'string' },
        imageAlign: { type: 'string' },
        text: { type: 'string' },
        teaser: { type: 'string' },
        phone: { type: 'object' },
        cta: { type: 'object' }
    },
    additionalProperties: false,
    required: ['title', 'image', 'text', 'teaser', 'cta']
};
