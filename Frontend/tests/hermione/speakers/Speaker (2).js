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
        return '-' + this.getColor() + '-' + this.platform + '   ' + this.getColor();
    }

    getInfo() {
        return {
            id: this.getId(),
            name: this.name,
            platform: this.platform,
        };
    }
};
