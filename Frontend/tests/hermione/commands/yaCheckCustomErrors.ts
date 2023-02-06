module.exports = async function yaCheckCustomErrors() {
    const customErrors = await this.execute(function () {
        // @ts-ignore
        return window.hermione?.customErrors;
    });

    if (customErrors?.length) {
        throw new Error(`Произошли ошибки:\n${customErrors.map(function (customError) {
            return customError.message + ' ' + customError.stack;
        }).join('\n')} \n`);
    }
};
