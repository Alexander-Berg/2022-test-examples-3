'use strict';

const _ = require('lodash');

class Nirvana {
    /**
     * @param {String} oAuthToken – OAuth-токен для авторизации в Нирване
     * @param {String} nirvanaHostName – хост Нирваны
     * @param {Object} logger – инстанс логгера
     */
    constructor(oAuthToken, nirvanaHostName, logger) {
        this.oAuthToken = oAuthToken;
        this.nirvanaHostName = nirvanaHostName;
        this.logger = logger;
    }

    startWorkflow() {
        return Promise.resolve();
    }

    getExecutionState() {
        return Promise.resolve({});
    }

    stopWorkflow() {
        return Promise.resolve();
    }

    stopWorkflowIfRunningOrWaiting() {
        return Promise.resolve();
    }

    setBlockPosition() {
        return Promise.resolve();
    }

    connectDataBlocks() {
        return Promise.resolve();
    }

    createData() {
        return Promise.resolve('81dcbae3-8c2f-4007-9c0f-' + Date.now());
    }

    editData() {
        return Promise.resolve();
    }

    uploadDataSync() {
        return Promise.resolve();
    }

    setGlobalParameters() {
        return Promise.resolve();
    }

    cloneWorkflow() {
        return Promise.resolve('98553b27-b003-11e6-98ff-' + Date.now());
    }

    getWorkflow() {
        return Promise.resolve({ connections: [] });
    }

    disconnectBlocks() {
        return Promise.resolve();
    }

    setBlockParameters() {
        return Promise.resolve();
    }

    /**
     * @param {String} workflowId
     * @param {Object[]} blocks
     */
    addDataBlocks(workflowId, blocks) {
        const rnd = String(Date.now());

        return Promise.resolve(_.range(blocks.length).map(() => ({
            blockGuid: '3ec5cf6b-f508-4d3a-8f71-' + rnd,
            blockCode: 'data-c7d2813d-9c09-4d9b-8d20-' + rnd,
        })));
    }

    getOrCreateUser() {
        return Promise.resolve();
    }
}

module.exports = Nirvana;
