const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: {
            content_type: 'form-line',
            content: {
                label: 'Комментарий',
                content_type: 'textarea',
                placeholder: 'Напишите ваш комментарий',
                validation: [{ required: { text: 'required' } }],
            },
        },
    },
});
