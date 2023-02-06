module.exports = function yaCheckCustomErrors() {
    return this
        .execute(function () {
            return window.hermione && window.hermione.customErrors;
        })
        .then((data) => {
            const customErrors = data.value;

            if (customErrors && customErrors.length) {
                throw new Error(`Произошли ошибки:\n${customErrors.map(function (customError) {
                    return customError.message + ' ' + customError.stack;
                }).join('\n')} \n`);
            }
        });
};
