'use strict';

const getHash = require('./get-hash.js');

describe('getHash', () => {
    it('на пустой url отдает пустую строку', () => {
        expect(getHash('')).toEqual('');
    });

    it('на странный url отдает пустую строку', () => {
        expect(getHash('asdasd')).toEqual('');
    });

    it('happy path', () => {
        expect(getHash('https://yadi.sk/mail/?hash=EjZ4FGXXXXX1tcmkADi4Xo2lrIShK8Q0IotUKszExw%3D'))
            .toEqual('EjZ4FGXXXXX1tcmkADi4Xo2lrIShK8Q0IotUKszExw=');
    });
});
