const Step = require('./Step');

module.exports = class ActionsStep extends Step {
    type = 'scenarios.steps.actions';

    constructor(devices = [], requestedSpeakerCapabilities = []) {
        const launchDevices = devices.map(device => {
            return {
                id: device.id,
                capabilities: device.capabilities,
            };
        });

        super({
            launch_devices: launchDevices,
            requested_speaker_capabilities: requestedSpeakerCapabilities,
        });
    }
};
