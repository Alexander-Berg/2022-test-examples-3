const Speaker = require('./Speaker');
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class YandexLight extends Speaker {
    constructor(name = 'Станция Лайт', color = Color.BLACK) {
        super(Platform.YANDEXMICRO, name, color);
    }

    getColor() {
        switch (this.color) {
            case Color.GREEN:
                return 'G';
            case Color.PURPLE:
                return 'P';
            case Color.RED:
                return 'R';
            case Color.BEIGE:
                return 'B';
            case Color.YELLOW:
                return 'Y';
            case Color.PINK:
                return 'N';
        }
    }
};
