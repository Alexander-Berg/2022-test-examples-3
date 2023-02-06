module.exports = async (bro) => {
    return await bro.execute(() => {
        const activeElement = document.activeElement;
        if (activeElement) {
            activeElement.blur();
        }
    });
};
