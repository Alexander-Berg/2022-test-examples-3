const getMiddleware = require('../../../../app/middleware/collect-extra-user-params');
const ONE_DAY = 24 * 60 * 60 * 1000;
const originalDateNow = global.Date.now;

beforeEach(() => {
    global.Date.now = jest.fn();
});

afterEach(() => {
    global.Date.now = originalDateNow;
});

describe('collect-extra-user-params middleware', () => {
    it('должен выставить `loginMd5`, если есть авторизация', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({ data: { settings: { verstka: {} } } }),
                userFeatures: jest.fn().mockResolvedValue(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.user.loginMd5).toBe('4507f3ed3900f3cb24ce32230e8bccb7');
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            expect(req.intapi.getPSReadonlySettings).toHaveBeenCalled();
            done();
        });
    });

    it('не должен выставить `loginMd5`, если нет ни авторизации, ни сохраненного `uid`', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {},
            cookies: {},
            mpfs: {
                userInfo: jest.fn(),
                userFeatures: jest.fn(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn()
            }
        };

        middleware(req, null, () => {
            expect(req.user.loginMd5).toBeUndefined();
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            expect(req.intapi.getPSReadonlySettings).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен корректно выставить типы листинга из пользовательских настроек, если есть `uid`', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({
                    data: { settings: { verstka: { typeListingWowPublic: 'wow', typeListingPublic: 'no-wow' } } },
                }),
                userFeatures: jest.fn().mockResolvedValue(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.user.settings).toEqual({
                typeListingWowPublic: 'wow',
                typeListingPublic: 'no-wow',
            });
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен корректно выставить типы листинга из кук, если нет `uid`', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {},
            cookies: { typeListingWowPublic: 'wow', typeListingPublic: 'no-wow' },
            mpfs: {
                userInfo: jest.fn(),
                userFeatures: jest.fn(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn()
            }
        };

        middleware(req, null, () => {
            expect(req.user.settings).toEqual({
                typeListingWowPublic: 'wow',
                typeListingPublic: 'no-wow',
            });
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('не должен выставлять типы листинга, если нет ни `uid`, ни кук', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {},
            cookies: {},
            mpfs: {
                userInfo: jest.fn(),
                userFeatures: jest.fn(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn()
            }
        };

        middleware(req, null, () => {
            expect(req.user.settings).toEqual({});
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен корректно сработать в случае ошибки при получении пользовательских данных', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            mpfs: {
                userInfo: jest.fn().mockRejectedValue({}),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.user.settings).toEqual({});
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен выставить фичи, если есть uid', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({}),
                userFeatures: jest
                    .fn()
                    .mockResolvedValue({ data: { advertising: { enabled: true }, antifo: { enabled: false } } }),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.visitorFeatures).toEqual({
                advertizingDisabled: false,
                antifoDisabled: true,
            });
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен выставить настройки из датасинка если коллекция есть', (done) => {
        const middleware = getMiddleware();

        const req = {
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({}),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({
                    datasync_ro_no_b2c_tariff_promo: { enabled: true, value: undefined }
                })
            }
        };

        middleware(req, null, () => {
            expect(req.user.psSettings).toEqual({
                enabledNoB2cTariffPromo: true
            });

            done();
        });
    });

    it('не должен выставить фичи, если нет uid', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {},
            cookies: {},
            mpfs: {
                userInfo: jest.fn(),
                userFeatures: jest.fn(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn()
            }
        };

        middleware(req, null, () => {
            expect(req.visitorFeatures).toEqual({});
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('не должен показывать промобаннер для неподдерживаемой ОС', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Linux' },
            user: {},
            cookies: {},
            mpfs: {
                userInfo: jest.fn(),
                userFeatures: jest.fn(),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn()
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBeUndefined();
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен показать баннер ПО, если явно проставлен параметр `show-desktop-banner=1`', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            query: { 'show-desktop-banner': '1' },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({}),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBe('desktop');
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен показать баннер ПО, нет uid', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {},
            cookies: {},
            query: {},
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({}),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBe('desktop');
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('не должен показать баннер ПО, явно указан параметр `show-desktop-banner=0`', (done) => {
        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {},
            cookies: {},
            query: { 'show-desktop-banner': '0' },
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({}),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn(),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBeUndefined();
            expect(req.mpfs.userInfo).not.toHaveBeenCalled();
            expect(req.mpfs.userFeatures).not.toHaveBeenCalled();
            expect(req.mpfs.userActivityInfo).not.toHaveBeenCalled();
            done();
        });
    });

    it('должен показать промо баннер ПО, если выполнились все условия', (done) => {
        const now = 1634218800000;
        const lastActivity = now - ONE_DAY * 61;
        const timestampClosePromoBannerPublic = now - ONE_DAY * 31;
        const timestampClickButtonPromoBannerPublic = now - ONE_DAY * 3;

        global.Date.now.mockReturnValueOnce(now);

        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            query: {},
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({
                    data: {
                        settings: {
                            verstka: {
                                timestampClosePromoBannerPublic,
                                timestampClickButtonPromoBannerPublic,
                            },
                        },
                    },
                }),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn().mockResolvedValue({
                    data: {
                        windows: { last_activity: lastActivity },
                    },
                }),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBe('desktop');
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).toHaveBeenCalledWith(15);
            done();
        });
    });

    it('не должен показать промо баннер ПО, если не соблюден таймаут активности', (done) => {
        const now = 1634218800000;
        const lastActivity = now - ONE_DAY * 59;
        const timestampClosePromoBannerPublic = now - ONE_DAY * 31;
        const timestampClickButtonPromoBannerPublic = now - ONE_DAY * 3;

        global.Date.now.mockReturnValueOnce(now);

        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            query: {},
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({
                    data: {
                        settings: {
                            verstka: {
                                timestampClosePromoBannerPublic,
                                timestampClickButtonPromoBannerPublic,
                            },
                        },
                    },
                }),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn().mockResolvedValue({
                    data: {
                        windows: { last_activity: lastActivity },
                    },
                }),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBeUndefined();
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).toHaveBeenCalledWith(15);
            done();
        });
    });

    it('не должен показать промо баннер ПО, не соблюден таймаут скрытия', (done) => {
        const now = 1634218800000;
        const lastActivity = now - ONE_DAY * 61;
        const timestampClosePromoBannerPublic = now - ONE_DAY * 29;
        const timestampClickButtonPromoBannerPublic = now - ONE_DAY * 3;

        global.Date.now.mockReturnValueOnce(now);

        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            query: {},
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({
                    data: {
                        settings: {
                            verstka: {
                                timestampClosePromoBannerPublic,
                                timestampClickButtonPromoBannerPublic,
                            },
                        },
                    },
                }),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn().mockResolvedValue({
                    data: {
                        windows: { last_activity: lastActivity },
                    },
                }),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBeUndefined();
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).toHaveBeenCalledWith(15);
            done();
        });
    });

    it('не должен показать промо баннер ПО, не соблюден таймаут перехода', (done) => {
        const now = 1634218800000;
        const lastActivity = now - ONE_DAY * 61;
        const timestampClosePromoBannerPublic = 0;
        // eslint-disable-next-line no-implicit-coercion
        const timestampClickButtonPromoBannerPublic = now - ONE_DAY;

        global.Date.now.mockReturnValueOnce(now);

        const middleware = getMiddleware();

        const req = {
            ua: { OSFamily: 'Windows' },
            user: {
                id: 15,
                auth: {},
                normalizedLogin: 'yandex.user',
            },
            query: {},
            mpfs: {
                userInfo: jest.fn().mockResolvedValue({
                    data: {
                        settings: {
                            verstka: {
                                timestampClosePromoBannerPublic,
                                timestampClickButtonPromoBannerPublic,
                            },
                        },
                    },
                }),
                userFeatures: jest.fn().mockResolvedValue({}),
                userActivityInfo: jest.fn().mockResolvedValue({
                    data: {
                        windows: { last_activity: lastActivity },
                    },
                }),
            },
            intapi: {
                getPSReadonlySettings: jest.fn().mockResolvedValue({})
            }
        };

        middleware(req, null, () => {
            expect(req.bannerType).toBeUndefined();
            expect(req.mpfs.userInfo).toHaveBeenCalledWith(15);
            expect(req.mpfs.userFeatures).toHaveBeenCalledWith(15);
            expect(req.mpfs.userActivityInfo).toHaveBeenCalledWith(15);
            done();
        });
    });
});
