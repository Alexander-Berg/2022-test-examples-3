const Speaker = require('./Speaker');
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class YandexStationMax extends Speaker {
    constructor(name = 'Станция Макс', color = Color.BLACK) {
        super(Platform.YANDEXSTATION_2, name, color);
    }

    getColor() {
        switch (this.color) {
            case Color.BLACK:
                return 'K';
            case Color.WHITE:
                return 'W';
            case Color.BLUE:
                return 'B';
            case Color.RED:
                return 'R';
        }
    }
};
