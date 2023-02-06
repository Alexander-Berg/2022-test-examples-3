const data = require('../../tools/data');

module.exports = data.createPage(
    {
        content: [
            {
                content_type: 'button',
                on: {
                    click: [
                        {
                            id: 'callback_modal_form_id_0',
                            method: 'open',
                        },
                    ],
                },
                target: '_blank',
                text: 'Открой модальное окно',
            },
            {
                id: 'callback_modal_form_id_0',
                content_type: 'modal',
                title: 'Обратный звонок',
                content: {
                    content_type: 'form',
                    on: {
                        success: [
                            {
                                id: 'callback_modal_form_id_0',
                                method: 'close',
                            },
                        ],
                    },
                    target: '_top',
                    __form_callback_meta: {
                        callback_email: 'company@mail.ru',
                    },
                    content: [
                        {
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
                                    block: 'br',
                                },
                                {
                                    content: [
                                        {
                                            content: {
                                                on: {
                                                    click: [
                                                        {
                                                            id: 'callback_modal_form_id_0',
                                                            method: 'close',
                                                        },
                                                    ],
                                                },
                                                content_type: 'button',
                                                text: 'Закрыть',
                                            },
                                            block: 'column',
                                        },
                                        {
                                            content: {
                                                text: 'Отправить',
                                                style: {
                                                    color: '#fff',
                                                    'background-color': '#0078f0',
                                                },
                                                type: 'submit',
                                                content_type: 'button',
                                            },
                                            block: 'column',
                                        },
                                    ],
                                    content_type: 'fieldset',
                                },
                            ],
                            content_type: 'fieldset',
                        },
                    ],
                    ajax: true,
                    action: '/',
                    method: 'POST',
                },
            },
        ],
    }
);
