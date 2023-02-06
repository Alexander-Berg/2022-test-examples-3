const Speaker = require('./Speaker');
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class YandexStationMidi extends Speaker {
    constructor(name = 'Станция 2 поколения', color = Color.BLACK) {
        super(Platform.YANDEXMIDI, name, color);
    }

    getColor() {
        switch (this.color) {
            case Color.BLACK:
                return 'K';
            case Color.BEIGE:
                return 'E';
            case Color.BLUE:
                return 'B';
            case Color.COPPER:
                return 'C';
        }
    }
};
