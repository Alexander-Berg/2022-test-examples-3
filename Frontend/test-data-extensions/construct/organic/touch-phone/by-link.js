module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'test',
            blocks: [
                {
                    block: 'organic',
                    favicon: { domain: 'ya.ru' },
                    title: 'Найдено по ссылке',
                    path: [
                        { text: 'yandex.ru' }
                    ],
                    'by-link': {
                        block: 'html',
                        content: '<b>ololo</b> пыщь пыщь'
                    }
                }
            ]
        }
    }
};
