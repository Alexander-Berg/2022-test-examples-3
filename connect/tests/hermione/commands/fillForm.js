const formSelectors = require('../models/formSelectors');

module.exports = function(formName, data) {
    const selectorsMap = formSelectors[formName];

    return Object.keys(data).reduce((ctx, key) => {
        const value = selectorsMap[key];

        if (Array.isArray(value)) {
            return value.reduce((prevCtx, selector) =>
                prevCtx.clearInput(`.${formName} ${selector}`).setValue(`.${formName} ${selector}`, data[key]), ctx);
        }

        return ctx.clearInput(`.${formName} ${value}`).setValue(`.${formName} ${value}`, data[key]);
    }, this);
};
