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
                    question: 'Длина экватора земли',
                    answer: '40 075,676 км'
                }
            ]
        }
    }
};
