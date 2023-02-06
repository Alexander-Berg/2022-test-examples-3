const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: [
            {
                content_type: 'form-line',
                content: {
                    content_type: 'checkbox',
                    text: 'Да, я согласен',
                },
            },
            {
                content_type: 'form-line',
                content: {
                    content_type: 'input',
                    placeholder: 'Ваше имя',
                    type: 'text',
                    label: 'Как вас зовут',
                    validation: { required: true },
                },
            },
            {
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
                },
            },
            {
                content_type: 'form-line',
                content: {
                    label: 'Комментарий',
                    content_type: 'textarea',
                    placeholder: 'Напишите ваш комментарий',
                    validation: [
                        { required: true },
                    ],
                },
            },
        ],
    },
});
