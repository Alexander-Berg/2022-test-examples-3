const Trigger = require('./Trigger');

module.exports = class VoiceTrigger extends Trigger {
    type = 'scenario.trigger.voice';

    constructor(phrase = 'Фраза по умолчанию') {
        super(phrase);
    }
};
