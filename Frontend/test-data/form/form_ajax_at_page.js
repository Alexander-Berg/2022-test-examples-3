const data = require('../../tools/data');

module.exports = data.createSnippet({

    content_type: 'form',
    ajax: true,
    action: '/',
    method: 'POST',
    content: {
        content_type: 'fieldset',
        content: [
            {
                content: [
                    {
                        name: 'phone',
                        placeholder: '8-XXX-XXX-XX-XX',
                        label: 'Телефон',
                        content_type: 'input',
                        validation: {
                            phone: true,
                            required: true,
                        },
                        type: 'tel',
                    },
                ],
                content_type: 'form-line',
            },
            {
                content: [
                    {
                        validation: {
                            required: true,
                        },
                        placeholder: 'Иван Иванов',
                        name: 'name',
                        content_type: 'input',
                        label: 'Имя',
                    },
                ],
                content_type: 'form-line',
            },
            {
                content: [
                    {
                        name: 'comment',
                        content_type: 'textarea',
                        label: 'Вопрос',
                    },
                ],
                content_type: 'form-line',
            },
            {
                content: [
                    'Нажимая "Отправить", даю согласие "ООО Одуван" на обработку введенной информации и принимаю условия ',
                    {
                        url: 'http://oduvan.ru/',
                        text: 'Пользовательского соглашения',
                        content_type: 'link',
                    },
                ],
                content_type: 'agreement',
            },
            {
                content: [
                    {
                        text: 'Отправить',
                        style: {
                            color: '#fff',
                            'background-color': '#0078f0',
                        },
                        type: 'submit',
                        content_type: 'button',
                    },
                ],
                content_type: 'form-line',
            },
        ],
    },
});
