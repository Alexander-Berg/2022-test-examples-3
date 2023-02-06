module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        construct: {
            type: 'test',
            template: 'test',
            blocks: [
                {
                    block: 'fact',
                    title: {
                        text: '«Сбербанк» - Контактный центр',
                        url: '#'
                    },
                    answer: '8-800-555-55-50',
                    path: [
                        { text: 'www.sberbank.ru', url: '#1' },
                        { text: 'call_center', url: '#2' }
                    ],
                    phone: '8-800-555-55-50'
                }
            ]
        }
    }
};
