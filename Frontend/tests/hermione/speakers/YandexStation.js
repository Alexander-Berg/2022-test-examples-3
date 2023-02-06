const Speaker = require('./Speaker');
const { Platform } = require('./PlatformsAndColors');

module.exports = class YandexStation extends Speaker {
    constructor(name = 'Станция') {
        super(Platform.YANDEXSTATION, name);
    }
};
