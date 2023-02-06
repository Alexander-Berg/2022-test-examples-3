'use strict';

const original = Error.prepareStackTrace;
const override = (error, stack) => stack;

function getStack() {
    try {
        Error.prepareStackTrace = override;
        return new Error().stack;
    } finally {
        Error.prepareStackTrace = original;
    }

}

module.exports = getStack;
