const eventBus = require('./events').eventBus;

module.exports = (tide, config) => {
    tide.on(tide.events.END, () => {
        eventBus.emit('end');
    });
};
