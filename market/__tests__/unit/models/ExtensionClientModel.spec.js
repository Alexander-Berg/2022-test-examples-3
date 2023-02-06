'use strict';

const ExtensionClientModel = require('./../../../src/models/ExtensionClientModel');

describe('models / ExtensionClientModel', () => {
    describe('constructor', () => {
        it('should create an instance of ExtensionClientModel', () => {
            const ui = Math.random().toString();
            const version = Math.random().toString();
            const clientId = Date.now().toString();
            const scriptVersion = Math.random();
            const userAgent = Date.now().toString();
            const yandexuid = Math.random().toString();
            const yandexLogin = Math.random().toString();

            const clientModel = new ExtensionClientModel({
                ui,
                version,
                clientId,
                scriptVersion,
                userAgent,
                yandexuid,
                yandexLogin
            });

            expect(clientModel).toBeInstanceOf(ExtensionClientModel);
        });

        it('should throw a TypeError if ui is non-string', () => {
            const fn = () => {
                const ui = Math.random();
                new ExtensionClientModel({ ui });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if version in non-string', () => {
            const fn = () => {
                const version = Math.random();
                new ExtensionClientModel({ version });
            };

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#ui', () => {
        describe('get', () => {
            it('should return correct ui', () => {
                const ui = Math.random().toString();
                const clientModel = new ExtensionClientModel({ ui });

                expect(clientModel.ui).toBe(ui);
            });
        });

        describe('set', () => {
            it('should not thrown an error if ui is not defined', () => {
                const fn = () => {
                    const clientModel = new ExtensionClientModel();

                    clientModel.ui = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if ui is a string', () => {
                const fn = () => {
                    const ui = Math.random().toString();
                    const clientModel = new ExtensionClientModel();

                    clientModel.ui = ui;
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if ui is non-string', () => {
                const fn = () => {
                    const ui = Math.random();
                    const clientModel = new ExtensionClientModel();

                    clientModel.ui = ui;
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#version', () => {
        describe('get', () => {
            it('should return correct version', () => {
                const version = Math.random().toString();
                const clientModel = new ExtensionClientModel({ version });

                expect(clientModel.version).toBe(version);
            });
        });

        describe('set', () => {
            it('should not thrown an error if version is not defined', () => {
                const fn = () => {
                    const clientModel = new ExtensionClientModel();

                    clientModel.version = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if version is a string', () => {
                const fn = () => {
                    const version = Math.random().toString();
                    const clientModel = new ExtensionClientModel();

                    clientModel.version = version;
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if version is non-string', () => {
                const fn = () => {
                    const version = Math.random();
                    const clientModel = new ExtensionClientModel();

                    clientModel.version = version;
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });
});
