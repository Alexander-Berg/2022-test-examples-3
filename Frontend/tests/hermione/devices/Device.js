module.exports = class Device {
    capabilities = [];
    properties = [];
    manufacturer = 'Hermione';
    sw_version = '1.2.4_0016';

    constructor(name) {
        this.name = name;
    }

    addCapabilities(value) {
        this.capabilities.push(value);
    }

    addProperties(value) {
        this.properties.push(value);
    }

    addPropertyVoltage() {
        this.addProperties({
            type: 'devices.properties.float',
            retrievable: true,
            reportable: true,
            parameters: {
                instance: 'voltage',
                name: 'текущее напряжение',
                unit: 'unit.volt',
            },
        });
    }
    addPropertyPower() {
        this.addProperties({
            type: 'devices.properties.float',
            retrievable: true,
            reportable: true,
            parameters: {
                instance: 'power',
                name: 'потребляемая мощность',
                unit: 'unit.watt',
            },
        });
    }
    addPropertyAmperage() {
        this.addProperties({
            type: 'devices.properties.float',
            retrievable: true,
            reportable: true,
            parameters: {
                instance: 'amperage',
                name: 'потребление тока',
                unit: 'unit.ampere',
            },
        });
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
    addCapabilitiesColorSettingWithScenes() {
        this.addCapabilities({
            retrievable: true,
            type: 'devices.capabilities.color_setting',
            parameters: {
                color_model: 'rgb',
                temperature_k: {
                    min: 1700,
                    max: 6500,
                },
                color_scene: {
                    scenes: [
                        'party',
                        'night',
                        'romance',
                        'candle',
                        'siren',
                        'alarm',
                        'fantasy',
                        'reading',
                        'alice',
                        'jungle',
                        'neon',
                        'ocean',
                    ].map(item => ({ id: item })),
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
            properties: this.properties,
            type: this.type,
            device_info: {
                manufacturer: this.manufacturer,
                model: this.model,
                sw_version: this.sw_version,
            },
            custom_data: {
                message: 'Создано из Hermione',
                parent_end_point_id: '',
            },
        };
    }
};
