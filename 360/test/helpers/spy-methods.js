'use strict';

module.exports = function(sinon, object, methods) {
    methods.forEach((methodName) => sinon.spy(object, methodName));
};
