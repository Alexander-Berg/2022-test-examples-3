'use strict';

const SettingsModel = require('../../../src/models/RequestModel/SettingsModel');

describe('model / SettingsModel', () => {
    describe('constructor', () => {
        it('should create an instance of SettingsModel if input parameters are not defined', () => {
            const settingsModel = new SettingsModel();

            expect(settingsModel).toBeInstanceOf(SettingsModel);
        });

        it('should create an instance of SettingsModel if input parameters are defined and correct', () => {
            const affId = Math.random();
            const clid = Math.random();
            const vid = Math.random();
            const installId = Math.random().toString();
            const installTime = Math.random();

            const settingsModel = new SettingsModel({ affId, clid, vid, installId, installTime });

            expect(settingsModel).toBeInstanceOf(SettingsModel);
        });

        it('should throw a TypeError if \'affId\' is non-number', () => {
            const fn = () => {
                const affId = Math.random().toString();

                new SettingsModel({ affId });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'clid\' is non-number', () => {
            const fn = () => {
                const clid = Math.random().toString();

                new SettingsModel({ clid });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'vid\' is non-number', () => {
            const fn = () => {
                const vid = Math.random().toString();

                new SettingsModel({ vid });
            };

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#fromString', () => {
        it('should create an instance of SettingsModel (1)', () => {
            const affId = Math.random();
            const clid = Math.random();
            const vid = Math.random();
            const installId = Math.random().toString();
            const installTime = Math.random();
            const text = JSON.stringify({ affId, clid, vid, installId, installTime });
            const settingsModel = SettingsModel.fromString(text);

            expect(settingsModel.affId).toBe(affId);
            expect(settingsModel.clid).toBe(clid);
            expect(settingsModel.vid).toBe(vid);
            expect(settingsModel.installId).toBe(installId);
            expect(settingsModel.installTime).toBe(installTime);
        });

        it('should create an instance of SettingsModel (2)', () => {
            const affId = Math.trunc(Math.random() * 100);
            const clid = Math.trunc(Math.random() * 100);
            const vid = Math.trunc(Math.random() * 100);
            const installId = Math.random().toString();
            const installTime = Math.trunc(Math.random() * 100);

            const text = JSON.stringify({
                affId: affId.toString(),
                clid: clid.toString(),
                vid: vid.toString(),
                installId: installId,
                installTime: installTime.toString()
            });

            const settingsModel = SettingsModel.fromString(text);

            expect(settingsModel.affId).toBe(affId);
            expect(settingsModel.clid).toBe(clid);
            expect(settingsModel.vid).toBe(vid);
            expect(settingsModel.installId).toBe(installId);
            expect(settingsModel.installTime).toBe(installTime);
        });

        it('should create an instance of SettingsModel (3)', () => {
            const affId = Math.trunc(Math.random() * 100);
            const clid = Math.trunc(Math.random() * 100);
            const vid = Math.trunc(Math.random() * 100);

            const text = JSON.stringify({
                affId: affId,
                clid: `${clid.toString()}-${vid.toString()}`
            });

            const settingsModel = SettingsModel.fromString(text);

            expect(settingsModel.affId).toBe(affId);
            expect(settingsModel.clid).toBe(clid);
            expect(settingsModel.vid).toBe(vid);
        });

        it('should create an instance of SettingsModel (4)', () => {
            const affId = Math.trunc(Math.random() * 100);
            const clid = Math.trunc(Math.random() * 100);

            const text = JSON.stringify({
                affId: affId,
                clid: clid.toString()
            });

            const settingsModel = SettingsModel.fromString(text);

            expect(settingsModel.affId).toBe(affId);
            expect(settingsModel.clid).toBe(clid);
            expect(settingsModel.vid).toBeUndefined();
        });

        it('should create an instance of SettingsModel (4)', () => {
            const text = JSON.stringify({});
            const settingsModel = SettingsModel.fromString(text);

            expect(settingsModel.affId).toBeUndefined();
            expect(settingsModel.clid).toBeUndefined();
            expect(settingsModel.vid).toBeUndefined();
        });

        it('should throw a SyntaxError if \'text\' is incorrect', () => {
            const text = '';
            const fn = () => SettingsModel.fromString(text);

            expect(fn).toThrow(SyntaxError);
        });

        it('should throw a TypeError if \'affId\' is incorrect', () => {
            const text = JSON.stringify({
                affId: []
            });

            const fn = () => SettingsModel.fromString(text);

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'clid\' is incorrect', () => {
            const text = JSON.stringify({
                clid: 'asd-asd'
            });

            const fn = () => SettingsModel.fromString(text);

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'installId\' is incorrect', () => {
            const text = JSON.stringify({
                installId: null
            });

            const fn = () => SettingsModel.fromString(text);

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError id \'installTime\' is incorrect', () => {
            const text = JSON.stringify({
                installTime: []
            });

            const fn = () => SettingsModel.fromString(text);

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#affId', () => {
        it('should return \'undefined\' by default', () => {
            const settingsModel = new SettingsModel();

            const actual = settingsModel.affId;
            expect(actual).toBeUndefined();
        });

        it('should return correct value', () => {
            const affId = Math.random();
            const settingsModel = new SettingsModel({ affId });

            const actual = settingsModel.affId;
            expect(actual).toBe(affId);
        });
    });

    describe('#clid', () => {
        it('should return \'undefined\' by default', () => {
            const settingsModel = new SettingsModel();

            const actual = settingsModel.clid;
            expect(actual).toBeUndefined();
        });

        it('should return correct value (1)', () => {
            const clid = Math.random();
            const settingsModel = new SettingsModel({ clid });

            const actual = settingsModel.clid;
            expect(actual).toBe(clid);
        });

        it('should return correct value (2)', () => {
            const clid = Math.trunc(Math.random() * 100);
            const vid = Math.trunc(Math.random() * 100);

            const text = JSON.stringify({
                clid: `${clid}-${vid}`
            });

            const settingsModel = SettingsModel.fromString(text);

            const actual = settingsModel.clid;
            expect(actual).toBe(clid);
        });
    });

    describe('#vid', () => {
        it('should return \'undefined\' by default', () => {
            const settingsModel = new SettingsModel();

            const actual = settingsModel.vid;
            expect(actual).toBeUndefined();
        });

        it('should return correct value (1)', () => {
            const vid = Math.random();
            const settingsModel = new SettingsModel({ vid });

            const actual = settingsModel.vid;
            expect(actual).toBe(vid);
        });

        it('should return correct value (2)', () => {
            const clid = Math.trunc(Math.random() * 100);
            const vid = Math.trunc(Math.random() * 100);

            const text = JSON.stringify({
                clid: `${clid}-${vid}`
            });

            const settingsModel = SettingsModel.fromString(text);

            const actual = settingsModel.vid;
            expect(actual).toBe(vid);
        });
    });

    describe('#installId', () => {
        it('should return undefined by default', () => {
            const settingsModel = new SettingsModel();

            const actual = settingsModel.installId;
            expect(actual).toBeUndefined();
        });

        it('should return correct value', () => {
            const installId = Math.random().toString();
            const settingsModel = new SettingsModel({ installId });

            const actual = settingsModel.installId;
            expect(actual).toBe(installId);
        });
    });

    describe('#installTime', () => {
        it('should return undefined by default', () => {
            const settingsModel = new SettingsModel();

            const actual = settingsModel.installTime;
            expect(actual).toBeUndefined();
        });

        it('should return correct value (1)', () => {
            const installTime = Math.random();
            const settingsModel = new SettingsModel({ installTime });

            const actual = settingsModel.installTime;
            expect(actual).toBe(installTime);
        });

        it('should return correct value (2)', () => {
            const installTime = Math.trunc(Math.random() * 100);
            const text = JSON.stringify({ installTime: installTime.toString() });

            const settingsModel = SettingsModel.fromString(text);

            const actual = settingsModel.installTime;
            expect(actual).toBe(installTime);
        });
    });
});
