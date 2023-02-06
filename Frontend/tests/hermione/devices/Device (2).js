module.exports = class Device {
    capabilities = [];
    manufacturer = 'Hermione';
    sw_version = '1.2.4_0016';

    constructor(name) {
        this.name = name;
    }

    addCapabilities(value) {
        this.capabilities.push(value);
    }

    addCapabilitiesColorSetting() {
        this.addCapabilities({
            retrievable: true,
            type: 'devices.capabilities.color_setting',
            parameters: {
                color_model: 'rgb',
                temperature_k: {
                    min: 1700,
                    max: 6500,
                },
            },
        });
    }

    addCapabilitiesOnOff() {
        this.addCapabilities({
            retrievable: true,
            type: 'devices.capabilities.on_off',
            parameters: {
                split: false,
            },
        });
    }

    addCapabilitiesRange() {
        this.addCapabilities({
            retrievable: true,
            type: 'devices.capabilities.range',
            parameters: {
                instance: 'brightness',
                unit: 'unit.percent',
                random_access: true,
                looped: false,
                range: {
                    min: 1,
                    max: 100,
                    precision: 1,
                },
            },
        });
    }

    getInfo() {
        return {
            id: 'hermione-device-' + Math.random(),
            name: this.name,
            capabilities: this.capabilities,
            'properties': [],
            type: this.type,
            device_info: {
                manufacturer: this.manufacturer,
                model: this.model,
                sw_version: this.sw_version,
            },
            custom_data: {
                message: 'Создано из Hermione',
            },
        };
    }
};
