module.exports = class Queue {
    constructor() {
        this._promise = Promise.resolve();
    }

    add(task) {
        this._promise = this._promise.then(task);
    }

    promise() {
        return this._promise;
    }
};
