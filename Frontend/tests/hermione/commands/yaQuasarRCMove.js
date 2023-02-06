module.exports = async function(direction) {
    await this.yaQuasarRunCommand('navigation', {
        direction,
        origin: 'touch',
    });

    await new Promise((resolve) => setTimeout(resolve, 1000));
};
