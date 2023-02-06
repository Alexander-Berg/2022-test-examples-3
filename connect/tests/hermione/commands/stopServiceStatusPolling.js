module.exports = function stopServiceStatusPolling() {
    return this.execute(() => {
        window.ya.connect.ServiceStatusPolling.stop();
    });
};
