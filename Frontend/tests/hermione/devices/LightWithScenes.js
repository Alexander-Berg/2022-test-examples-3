const Device = require('./Device');

module.exports = class LightWithScenes extends Device {
    type = 'devices.types.light';
    model = 'Hermione Lamp with Scenes';

    constructor(name = 'Лампочка Hermione') {
        super(name);

        this.addCapabilitiesOnOff();
        this.addCapabilitiesRange();
        this.addCapabilitiesColorSettingWithScenes();
    }
};
