module.exports = async function yaCrashOnReadonly(message) {
    const readonly = await this.getMeta('readonly');

    if (readonly) {
        console.error(message);
        throw new Error(message);
    }
};
