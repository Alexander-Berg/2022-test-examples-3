const Device = require('./Device');

module.exports = class Socket extends Device {
    type = 'devices.types.socket';
    model = 'Hermione Socket';

    constructor(name = 'Розетка Hermione') {
        super(name);

        this.addCapabilitiesOnOff();
    }
};
