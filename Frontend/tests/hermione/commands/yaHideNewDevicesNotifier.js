module.exports = async function yaHideNewDevicesNotifier(hide = true) {
    await this.execute(function(hide) {
        Array.from(document.getElementsByClassName('iot-tile-item__notifier-bullet'))
            .forEach(e => e.style.display = hide ? 'none' : 'block');
    }, hide);
};
