module.exports = function(name, payload) {
    return this.execute((name, payload) => {
        window.quasar.emitCommand(name, payload);
    }, name, payload);
};
