const _ = require('lodash');
const BaseFactory = require('tests/db/factory/base');

class ImageFactory extends BaseFactory {
    static get defaultData() {
        return {
            imageId: '1234/1234',
            name: 'test.jpg',
        };
    }

    static unique(data) {
        return _.pick(data, ['imageId']);
    }

    static get table() {
        return require('db').image;
    }
}

module.exports = ImageFactory;
