const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: {
            content_type: 'form-line',
            content: {
                content_type: 'checkbox',
                text: 'Да, я согласен',
                validation: [{ required: { text: 'required' } }],
            },
        },
    },
});
