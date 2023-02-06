const Speaker = require('./Speaker');
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class YandexMini2 extends Speaker {
    constructor(name = 'Станция Мини 2', color = Color.BLACK) {
        super(Platform.YANDEXMINI_2, name, color);
    }

    getColor() {
        switch (this.color) {
            case Color.BLACK:
                return 'K';
            case Color.GRAY:
                return 'G';
            case Color.BLUE:
                return 'B';
            case Color.RED:
                return 'R';
        }
    }
};
