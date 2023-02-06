'use strict';

const AbModel = require('./../../../src/models/ClientModel/AbModel');
const DistributionModel = require('./../../../src/models/ClientModel/DistributionModel');
const UserPreferencesModel = require('../../../src/models/ClientModel/UserPreferencesModel');
const ClientModel = require('./../../../src/models/ClientModel');

describe('models / ClientModel', () => {
    describe('constructor', () => {
        it('should create an instance of ClientModel', () => {
            const clientId = Date.now().toString();
            const scriptVersion = Math.random();
            const distributionModel = new DistributionModel();
            const userPreferencesModel = new UserPreferencesModel();
            const abModel = new AbModel();
            const userAgent = Date.now().toString();
            const isMobile = true;

            const clientModel = new ClientModel({
                clientId,
                scriptVersion,
                distributionModel,
                userPreferencesModel,
                abModel,
                userAgent,
                isMobile
            });

            expect(clientModel).toBeInstanceOf(ClientModel);
        });

        it('should throw a TypeError if clientId is non-string', () => {
            const clientId = Math.random();
            const fn = () => new ClientModel({ clientId });

            expect(fn).toThrow(TypeError);
        });

        it('should not throw an error if clientId is not defined', () => {
            const fn = () => new ClientModel();

            expect(fn).not.toThrow();
        });

        it('should not throw an error if clientId is string', () => {
            const clientId = Date.now().toString();
            const fn = () => new ClientModel({ clientId });

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if scriptVersion is non-number', () => {
            const scriptVersion = Date.now().toString();
            const fn = () => new ClientModel({ scriptVersion });

            expect(fn).toThrow(TypeError);
        });

        it('should not throw an error if scriptVersion is number', () => {
            const scriptVersion = Math.random();
            const fn = () => new ClientModel({ scriptVersion });

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if distributionModel is not an instance of DistributionModel', () => {
            const distributionModel = Date.now().toString();
            const fn = () => new ClientModel({ distributionModel });

            expect(fn).toThrow(TypeError);
        });

        it('should not throw an error if distributionModel is an instance of DistributionModel', () => {
            const distributionModel = new DistributionModel();
            const fn = () => new ClientModel({ distributionModel });

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if userPreferencesModel is not an instance of DistributionModel', () => {
            const userPreferencesModel = Date.now().toString();
            const fn = () => new ClientModel({ userPreferencesModel });

            expect(fn).toThrow(TypeError);
        });

        it('should not throw an error if userPreferencesModel is an instance of DistributionModel', () => {
            const userPreferencesModel = new UserPreferencesModel();
            const fn = () => new ClientModel({ userPreferencesModel });

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if abModel is not an instance of AbModel', () => {
            const abModel = Date.now().toString();
            const fn = () => new ClientModel({ abModel });

            expect(fn).toThrow(TypeError);
        });

        it('should not throw an error if abModel is an instance of AbModel', () => {
            const abModel = new AbModel();
            const fn = () => new ClientModel({ abModel });

            expect(fn).not.toThrow();
        });
    });

    describe('#abModel', () => {
        describe('set', () => {
            it('should throw a TypeError if abModel is not an instance of AbModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.abModel = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if abModel is an instance of AbModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.abModel = new AbModel();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#clientId', () => {
        describe('set', () => {
            it('should throw a TypeError if clientId is non-string', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.clientId = Math.random();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if clientId is string', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.clientId = Date.now().toString();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#userAgent', () => {
        describe('set', () => {
            it('should not throw an error if userAgent is not defined', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.userAgent = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if userAgent is string', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.userAgent = Math.random().toString();
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if userAgent is non-string', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.userAgent = Math.random();
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#distributionModel', () => {
        describe('set', () => {
            it('should throw a TypeError if distributionModel is not an instance of DistributionModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.distributionModel = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if distributionModel is an instance of DistributionModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.distributionModel = new DistributionModel();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#userPreferencesModel', () => {
        describe('set', () => {
            it('should throw a TypeError if userPreferencesModel is not an instance of DistributionModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.userPreferencesModel = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if userPreferencesModel is an instance of DistributionModel', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.userPreferencesModel = new UserPreferencesModel();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#scriptVersion', () => {
        describe('set', () => {
            it('should throw a TypeError if scriptVersion is non-number', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.scriptVersion = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if scriptVersion is number', () => {
                const fn = () => {
                    const clientModel = new ClientModel();
                    clientModel.scriptVersion = Math.random();
                };

                expect(fn).not.toThrow();
            });
        });
    });
});
