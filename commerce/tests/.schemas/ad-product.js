module.exports = {
    id: '/Ad-product',
    type: 'object',
    properties: {
        caption: { type: 'string' },
        teaser: { type: 'string' },
        text: { type: 'string' },
        title: { type: 'string' },
        items: { type: 'array' },
        cta: { type: 'object' },
        footer: { type: 'object' },
        goals: { type: 'object' },
        modal: { type: 'object' },
        requirements: { type: 'object' },
        seo: { type: 'object' }
    },
    additionalProperties: false,
    required: [
        'caption',
        'cta',
        'footer',
        'goals',
        'items',
        'modal',
        'requirements',
        'teaser',
        'text',
        'title'
    ]
};
