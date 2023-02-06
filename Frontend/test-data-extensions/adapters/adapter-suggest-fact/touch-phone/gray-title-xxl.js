var extend = require('util')._extend,
    thumb = extend(require('../__datasets/yellow-thumb-170x170.dataset'), { shade: true });

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
                    thumb: thumb,
                    question: [
                        { text: 'Владимир Путин', url: '//ya.ru' },
                        'Родился'
                    ],
                    answer: '28 лет'
                }
            ]
        }
    }
};
