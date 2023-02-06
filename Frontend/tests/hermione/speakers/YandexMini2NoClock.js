const YandexMini2 = require('./YandexMini2');
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class YandexMini2NoClock extends YandexMini2 {
    constructor(name = 'Станция Мини 2 без часов', color = Color.BLACK) {
        super(Platform.YANDEXMINI_2, name, color);
    }
};
