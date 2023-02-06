module.exports = {
    type: 'snippet',
    data_stub: {
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
                    answer: '+7 (495) 124-73-13',
                    path: [
                        { text: 'www.sberbank.ru', url: '#1' },
                        { text: 'call_center', url: '#2' }
                    ],
                    phone: '+7 (495) 24-73-13'
                }
            ]
        }
    }
};
