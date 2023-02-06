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
        cta: { type: 'object' },
        seo: { type: 'object' },
        noindex: { type: 'boolean' }
    },
    additionalProperties: true,
    required: ['title', 'image', 'text', 'teaser', 'cta']
};
