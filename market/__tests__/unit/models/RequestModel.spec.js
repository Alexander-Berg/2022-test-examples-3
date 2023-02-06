'use strict';

const Url = require('./../../../src/entities/Url');
const SettingsModel = require('../../../src/models/RequestModel/SettingsModel');
const RequestModel = require('./../../../src/models/RequestModel');

describe('models / RequestModel', () => {
    describe('constructor', () => {
        it('should create an instance of RequestModel if input parameters are not defined', () => {
            const requestModel = new RequestModel();

            expect(requestModel).toBeInstanceOf(RequestModel);
        });

        it('should create an instance of RequestModel if input parameters are defined and correct', () => {
            const ip = Math.random().toString();
            const transactionId = Math.random().toString();
            const regionId = Math.random();
            const url = new Url('https://sovetnik.yandex.ru');
            const referrer = new Url('https://www.top-shop.ru');
            const settingsModel = new SettingsModel();
            const requestModel = new RequestModel({ ip, transactionId, regionId, url, referrer, settingsModel });

            expect(requestModel).toBeInstanceOf(RequestModel);
        });

        it('should throw a TypeError if \'transactionId\' is non-string', () => {
            const fn = () => {
                const transactionId = Math.random();
                new RequestModel({ transactionId });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'regionId\' is non-number', () => {
            const fn = () => {
                const regionId = Math.random().toString();
                new RequestModel({ regionId });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if referrer is not instance of Url', () => {
            const fn = () => {
                const referrer = 'https://sovetnik.market.yandex.ru';

                new RequestModel({ referrer });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if referrer is not instance of Url', () => {
            const fn = () => {
                const referrer = 'https://sovetnik.market.yandex.ru';

                new RequestModel({ referrer });
            };

            expect(fn).toThrow(TypeError);
        });

        it('should throw a TypeError if \'settingsModel\' is not instance of SettingsModel', () => {
            const fn = () => {
                const settingsModel = Math.random();

                new RequestModel({ settingsModel });
            };

            expect(fn).toThrow(TypeError);
        });
    });

    describe('#ip', () => {
        describe('set', () => {
            it('should not throw an error if ip is not defined', () => {
                const fn = () => new RequestModel();

                expect(fn).not.toThrow();
            });

            it('should not throw an error if ip is a string', () => {
                const fn = () => {
                    const ip = Math.random().toString();
                    new RequestModel({ ip });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if ip is non-string', () => {
                const fn = () => {
                    const ip = Math.random();
                    new RequestModel({ ip });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#transactionId', () => {
        describe('set', () => {
            it('should not throw an error if transaction is not defined', () => {
                const fn = () => new RequestModel();

                expect(fn).not.toThrow();
            });

            it('should not throw an error if transaction is string', () => {
                const fn = () => {
                    const transactionId = Math.random().toString();
                    new RequestModel({ transactionId });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if transactionId is non-string', () => {
                const fn = () => {
                    const transactionId = Math.random();
                    new RequestModel({ transactionId });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#regionId', () => {
        describe('set', () => {
            it('should not throw an error if \'regionId\' is not defined', () => {
                const fn = () => {
                    new RequestModel();
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if \'regionId\' is number', () => {
                const fn = () => {
                    const regionId = Math.random();
                    new RequestModel({ regionId });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if \'regionId\' is non-number', () => {
                const fn = () => {
                    const regionId = Math.random().toString();
                    new RequestModel({ regionId });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#referrer', () => {
        describe('set', () => {
            it('should not throw an error if referrer is not defined', () => {
                const fn = () => {
                    new RequestModel();
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if referrer is instance of Url', () => {
                const fn = () => {
                    const referrer = new Url('https://sovetnik.market.yandex.ru');
                    new RequestModel({ referrer });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if referrer is not instance of Url', () => {
                const fn = () => {
                    const referrer = 'https://sovetnik.market.yandex.ru';
                    new RequestModel({ referrer });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#settingsModel', () => {
        describe('get', () => {
            it('should return an instance of SettingsModel by default', () => {
                const requestModel = new RequestModel();

                const actual = requestModel.settingsModel;

                expect(actual).toBeInstanceOf(SettingsModel);
            });

            it('should return correct \'settingsModel\'', () => {
                const affId = Math.random();
                const clid = Math.random();
                const vid = Math.random();
                const settingsModel = new SettingsModel({ affId, clid, vid });
                const requestModel = new RequestModel({ settingsModel });

                const actual = requestModel.settingsModel;

                expect(actual).toEqual(settingsModel);
            });
        });

        describe('set', () => {
            it('shouldn\'t throw an error if \'settingsModel\' is undefined', () => {
                const fn = () => {
                    const requestModel = new RequestModel();
                    requestModel.settingsModel = undefined;
                };

                expect(fn).not.toThrow();
            });

            it('shouldn\'t throw an error if \'settingsModel\' is an instance of SettingsModel', () => {
                const fn = () => {
                    const requestModel = new RequestModel();
                    requestModel.settingsModel = new SettingsModel();
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if \'settingsModel\' is incorrect', () => {
                const fn = () => {
                    const requestModel = new RequestModel();
                    requestModel.settingsModel = Math.random();
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#url', () => {
        describe('set', () => {
            it('should not throw an error if url is not defined', () => {
                const fn = () => {
                    new RequestModel();
                };

                expect(fn).not.toThrow();
            });

            it('should not throw an error if url is instance of Url', () => {
                const fn = () => {
                    const url = new Url('https://sovetnik.market.yandex.ru');

                    new RequestModel({ url });
                };

                expect(fn).not.toThrow();
            });

            it('should throw a TypeError if url is not instance of Url', () => {
                const fn = () => {
                    const url = 'https://sovetnik.market.yandex.ru';

                    new RequestModel({ url });
                };

                expect(fn).toThrow(TypeError);
            });
        });
    });

    describe('#domain', () => {
        describe('get', () => {
            it('should return right domain of the request', () => {
                const url = new Url('https://www.some.shop.ru?yclid=11235813');
                const expected = 'some.shop.ru';
                const requestModel = new RequestModel({ url });
                const actual = requestModel.domain;

                expect(actual).toBe(expected);
            });

            it('should return undefined, if Model was created without url', () => {
                const requestModel = new RequestModel();
                const actual = requestModel.domain;

                expect(actual).toBeUndefined();
            });
        });
    });

    describe('#ymclid', () => {
        describe('get', () => {
            it('should parse and return right ymclid from url of the request', () => {
                const url = new Url('https://some.shop?ymclid=11235813');
                const expected = '11235813';
                const requestModel = new RequestModel({ url });
                const actual = requestModel.ymclid;

                expect(actual).toEqual(expected);
            });

            it('should return undefined if request model was created without url', () => {
                const requestModel = new RequestModel();
                const actual = requestModel.ymclid;

                expect(actual).toBeUndefined();
            });
        });
    });

    describe('#yclid', () => {
        describe('get', () => {
            it('should parse and return right yclid from url of the request', () => {
                const url = new Url('https://some.shop?yclid=11235813');
                const expected = '11235813';
                const requestModel = new RequestModel({ url });
                const actual = requestModel.yclid;

                expect(actual).toEqual(expected);
            });

            it('should return undefined if request model was created without url', () => {
                const requestModel = new RequestModel();
                const actual = requestModel.yclid;

                expect(actual).toBeUndefined();
            });
        });
    });

    describe('#secondLevelDomain', () => {
        describe('get', () => {
            it('should return right second level domain of the request', () => {
                const url = new Url('https://www.some.shop.ru?yclid=11235813');
                const expected = 'shop.ru';
                const requestModel = new RequestModel({ url });
                const actual = requestModel.secondLevelDomain;

                expect(actual).toEqual(expected);
            });

            it('should return undefined, if Model was created without url', () => {
                const requestModel = new RequestModel();
                const actual = requestModel.secondLevelDomain;

                expect(actual).toBeUndefined();
            });
        });
    });
});
