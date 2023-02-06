'use strict';

const screenProfiles = require('./screen-profiles');

class Argentum {
    /**
     * @param {String} argentumHostName – хост Argentum
     * @param {Object} logger – инстанс логгера
     */
    constructor(argentumHostName, logger) {
        this.host = argentumHostName;
        this.logger = logger;
    }

    getReactions(){
        return Promise.resolve([]);
    }

    addReaction() {
        return Promise.resolve({});
    }

    deleteReaction() {
        return Promise.resolve({});
    }

    getWinsAgainstControlSystem() {
        return Promise.resolve({
            valid: true,
            'control-system-id': '2',
            'left-system-id': '0',
            'right-system-id': '1',
            results: {
                bt: {
                    'left-system-score': 0.7584739089416946,
                    'p-value': 1.76854185732607e-10,
                    'query-count': 100,
                    'right-system-score': 0.8856497612369912,
                },
                'win-rate': {
                    'left-system-score': 0.765,
                    'p-value': 2.054534504909998e-12,
                    'query-count': 100,
                    'right-system-score': 0.8985000000000005,
                },
            },
        });
    }

    createTicket() {
        const th = 1000;
        const rnd = Math.floor(Math.random() * th);

        return Promise.resolve(rnd);
    }

    getScreenProfiles() {
        return Promise.resolve(screenProfiles);
    }

    startExperiment() {
        return Promise.resolve({ 'workflow-id': 42 });
    }

    stopExperiment() {
        return Promise.resolve();
    }

    cloneExperiment() {
        return Promise.resolve({
            'update-log': [],
            'main-params': {},
        });
    }
}

module.exports = Argentum;
