'use strict';

const Stats = require('./errors-budget-stats.js');

class OtherStats {
    constructor(v1 = 0, v2 = 0) {
        this.v1 = v1;
        this.v2 = v2;
    }
}

describe('add', () => {
    it('checks type', () => {
        const stats = new Stats();
        expect(() => {
            stats.add(new OtherStats());
        }).toThrow();
    });

    it('adds values', () => {
        const stats = new Stats({ requests: 41, retries: 1 });
        stats.add(new Stats({ requests: 1, retries: 1 }));
        expect(stats.requests).toEqual(42);
        expect(stats.retries).toEqual(2);
    });
});

describe('calcRatio', () => {
    it('empty', () => {
        const stats = new Stats();
        expect(stats.calcRatio()).toEqual(0);
    });

    it('almost empty', () => {
        const stats = new Stats({ requests: 1, retries: 1 });
        expect(stats.calcRatio()).toEqual(0);
    });

    it('not empty', () => {
        const stats = new Stats({ requests: 10, retries: 2 });
        expect(stats.calcRatio()).toEqual(0.2);
    });
});
