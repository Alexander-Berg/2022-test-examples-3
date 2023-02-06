const CRC32 = require('crc-32').str;
const { Platform, Color } = require('./PlatformsAndColors');

module.exports = class Speaker {
    /**
     *
     * @param platform {Platform}
     * @param name {string}
     * @param color {Color}
     */
    constructor(platform, name, color = Color.BLACK) {
        this.platform = platform;
        this.name = name;
        this.color = color;
    }

    getColor() {
        return 'K';
    }

    getId() {
        const nameHash = CRC32(this.name) >>> 0;

        // Формат такой, чтобы прошло определение цвета по алгоритму
        // utils/speaker/get-color-symbol.ts
        return `X${this.getColor()}-${nameHash}-${this.platform}`;
    }

    getInfo() {
        return {
            id: this.getId(),
            name: this.name,
            platform: this.platform,
        };
    }
};
