const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: {
            content_type: 'form-line',
            content: {
                content_type: 'input',
                placeholder: 'Длинный плейсхолдер инпута',
                type: 'search',
                value: 'Длинное значение внутри не должно накладываться на иконку',
            },
        },
    },
});
