module.exports = {
    id: '/Product',
    type: 'object',
    properties: {
        title: { type: 'string' },
        sectionTitle: { type: 'string' },
        image: { type: 'string' },
        imageAlign: { type: 'string' },
        text: { type: 'string' },
        teaser: { type: 'string' },
        cta: { type: 'object' },
        phone: { type: 'object' },
        noindex: { type: 'boolean' }
    },
    additionalProperties: false,
    required: ['title', 'sectionTitle', 'image', 'imageAlign', 'text', 'teaser', 'cta', 'phone']
};
