const eventsEnum = {
    BEFORE_FILE_READ: 'BEFORE_FILE_READ',
    NEW_BROWSER: 'NEW_BROWSER',
};

function onMethod() { return this; }

const commonToolProps = {
    events: eventsEnum,
    on: onMethod,
    once: onMethod,
};

const oldTool = {
    ...commonToolProps,
    isWorker: null,
};

const newTool = {
    ...commonToolProps,
    isWorker: () => true,
};

module.exports = {
    oldTool,
    newTool,
};
