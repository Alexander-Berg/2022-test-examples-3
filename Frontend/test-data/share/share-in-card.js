const data = require('../../tools/data');

module.exports = data.createDocument({
    block: 'markup',
    content: {
        block: 'cards',
        items: [{
            content_type: 'share',
        }],
    },
});
