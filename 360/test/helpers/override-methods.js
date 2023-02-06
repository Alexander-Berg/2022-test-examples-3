'use strict';

const emptyFunc = () => {};

module.exports = function(object, methods, bodies) {
    bodies = bodies || {};

    methods.forEach((methodName) => {
        object[methodName] = typeof bodies[methodName] === 'function' ? bodies[methodName] : emptyFunc;
    });
};
