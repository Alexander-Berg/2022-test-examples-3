const EventEmitter = require('events').EventEmitter;

const events = new EventEmitter();

module.exports = {
    eventBus: events,
    // Этот промис резолвится на событие end, которое емитит tide-spy-plugin
    tideCompletion: new Promise(resolve => {
        events.on('end', () => {
            resolve(true);
        });
    }),
};
