const data = require('../../tools/data');

module.exports = data.createSnippet({
    content_type: 'fieldset',
    content: {
        content_type: 'form',
        content: {
            content_type: 'form-line',
            content: {
                block: 'selection-filter',
                data: [
                    {
                        name: 'channel',
                        items: [
                            {
                                text: 'Все каналы',
                                value: '',
                            },
                            {
                                text: 'Первый',
                                value: '146',
                            },
                            {
                                text: 'Россия 1',
                                value: '711',
                            },
                            {
                                text: 'Культура',
                                value: '187',
                            },
                        ],
                    },
                    {
                        name: 'date',
                        items: [
                            {
                                text: 'Сейчас',
                                value: '',
                            },
                            {
                                text: 'Сегодня',
                                value: '2017-10-18',
                            },
                            {
                                text: 'Завтра',
                                value: '2017-10-19',
                            },
                            {
                                text: 'Пт, 20 октября',
                                value: '2017-10-20',
                            },
                            {
                                text: 'Сб, 21 октября',
                                value: '2017-10-21',
                            },
                        ],
                    },
                ],
            },
        },
    },
});
