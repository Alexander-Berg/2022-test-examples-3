const Speaker = require('./Speaker');
const { Platform } = require('./PlatformsAndColors');

module.exports = class YandexMini extends Speaker {
    constructor(name = 'Станция Мини') {
        super(Platform.YANDEXMINI, name);
    }
};
