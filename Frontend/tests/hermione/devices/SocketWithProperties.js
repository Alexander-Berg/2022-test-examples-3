const Device = require('./Device');

module.exports = class SocketWithProperties extends Device {
    type = 'devices.types.socket';
    model = 'Hermione Socket';

    constructor(name = 'Розетка Hermione') {
        super(name);

        this.addCapabilitiesOnOff();
        this.addPropertyVoltage();
        this.addPropertyAmperage();
        this.addPropertyPower();
    }
};
