const Device = require('./Device');

module.exports = class Light extends Device {
    type = 'devices.types.light';
    model = 'Hermione Lamp';

    constructor(name = 'Лампочка Hermione') {
        super(name);

        this.addCapabilitiesOnOff();
        this.addCapabilitiesRange();
        this.addCapabilitiesColorSetting();
    }
};
