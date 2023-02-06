const Speaker = require('./Speaker');
const { Platform } = require('./PlatformsAndColors');

module.exports = class YandexModule2 extends Speaker {
    constructor(name = 'Яндекс Модуль 2') {
        super(Platform.YANDEXMODULE_2, name);
    }
};
