const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: {
            content_type: 'form-line',
            content: {
                content_type: 'select',
                options: [
                    { text: 'Москва', value: 1 },
                    { text: 'Саратов', value: 2 },
                    { text: 'Магадан', value: 3 },
                    { text: 'Омск', value: 4 },
                ],
                value: 3,
                name: 'city',
                label: 'Ваш город',
                validation: [{ 'min-value': { value: 10 } }],
            },
        },
    },
});
