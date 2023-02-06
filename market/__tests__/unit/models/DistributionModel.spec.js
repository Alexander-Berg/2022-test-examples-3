'use strict';

const DistributionModel = require('../../../src/models/ClientModel/DistributionModel');

describe('models / DistributionModel', () => {
    describe('constructor', () => {
        it('should create an instance of DistributionModel', () => {
            const vid = Math.random();
            const clid = Math.random();
            const affId = Math.random();
            const installId = Date.now().toString();
            const installTime = Date.now();
            const distributionModel = new DistributionModel(clid, affId, vid, installId, installTime);

            expect(distributionModel).toBeInstanceOf(DistributionModel);
        });

        it('should throw a TypeError if clid is non-number', () => {
            const clid = Date.now().toString();
            const affId = Math.random();
            const fn = () => new DistributionModel(clid, affId);

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if clid is number', () => {
            const clid = Math.random();
            const affId = Math.random();
            const fn = () => new DistributionModel(clid, affId);

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if affId is non-number', () => {
            const clid = Math.random();
            const affId = Date.now().toString();
            const fn = () => new DistributionModel(clid, affId);

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if affId is number', () => {
            const clid = Math.random();
            const affId = Math.random();
            const fn = () => new DistributionModel(clid, affId);

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if vid is defined but not number', () => {
            const clid = Math.random();
            const affId = Math.random();
            const vid = Date.now().toString();
            const fn = () => new DistributionModel(clid, affId, vid);

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if vid is not defined', () => {
            const clid = Math.random();
            const affId = Math.random();
            const fn = () => new DistributionModel(clid, affId);

            expect(fn).not.toThrow();
        });

        it('shouldn\'t throw an error if vid is number', () => {
            const vid = Math.random();
            const clid = Math.random();
            const affId = Math.random();
            const fn = () => new DistributionModel(clid, affId, vid);

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if installId is non-string', () => {
            const installId = Math.random();
            const fn = () => new DistributionModel(undefined, undefined, undefined, installId);

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if installId is string', () => {
            const installId = Date.now().toString();
            const fn = () => new DistributionModel(undefined, undefined, undefined, installId);

            expect(fn).not.toThrow();
        });

        it('should throw a TypeError if installTime is non-number', () => {
            const installTime = Date.now().toString();
            const fn = () => new DistributionModel(undefined, undefined, undefined, undefined, installTime);

            expect(fn).toThrow(TypeError);
        });

        it('shouldn\'t throw an error if installTime is number', () => {
            const installTime = Date.now();
            const fn = () => new DistributionModel(undefined, undefined, undefined, undefined, installTime);

            expect(fn).not.toThrow();
        });
    });

    describe('#clid', () => {
        describe('set', () => {
            it('should throw a TypeError if clid is non-number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.clid = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if clid is number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.clid = Math.random();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#affId', () => {
        describe('set', () => {
            it('should throw a TypeError if affId is non-number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.affId = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if affId is number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.affId = Math.random();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#vid', () => {
        describe('set', () => {
            it('should throw a TypeError if vid is defined but not number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.vid = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if vid is not defined', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.vid = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('shouldn\'t throw an error if vid is number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.clid = Math.random();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#installId', () => {
        describe('set', () => {
            it('should throw a TypeError if installId is non-string', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.installId = Math.random();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if installId is string', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.installId = Date.now().toString();
                };

                expect(fn).not.toThrow();
            });
        });
    });

    describe('#installTime', () => {
        describe('set', () => {
            it('should throw a TypeError if installTime is non-number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.installTime = Date.now().toString();
                };

                expect(fn).toThrow(TypeError);
            });

            it('shouldn\'t throw an error if installTime is number', () => {
                const fn = () => {
                    const distributionModel = new DistributionModel();
                    distributionModel.installTime = Date.now();
                };

                expect(fn).not.toThrow();
            });
        });
    });
});
