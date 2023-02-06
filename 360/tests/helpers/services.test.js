const { getDownloaderHost, OUR_SERVICES_HASHMAP, getServiceUrl } = require('../../helpers/services');

jest.isolateModules(() => {
    jest.resetModules();
    jest.doMock('../../helpers/is-regtests', () => true);
    const getServiceUrlRegtests = require('../../helpers/services').getServiceUrl;

    describe('yastatic в regtests', () => {
        it('на sk', () => {
            expect(getServiceUrlRegtests('yastatic', 'sk', 'prestable', 'chemodan-123.regtests.dsp.yadi.sk'))
                .toEqual('https://chemodan-123.regtests.dsp.yandex.net');
        });
        it('на ru', () => {
            expect(getServiceUrlRegtests('yastatic', 'ru', 'prestable', 'chemodan-123.regtests.dsp.yandex.ru'))
                .toEqual('https://chemodan-123.regtests.dsp.yandex.net');
        });
        it('на com.tr', () => {
            expect(getServiceUrlRegtests('yastatic', 'tr', 'prestable', 'chemodan-123.regtests.dsp.yandex.com.tr'))
                .toEqual('https://chemodan-123.regtests.dsp.yandex.net');
        });
        it('на yadi.sk (ассесорская прокси)', () => {
            expect(getServiceUrlRegtests('yastatic', 'sk', 'prestable', 'yadi.sk'))
                .toEqual('https://disk.yandex.net');
        });
    });
});

describe('services helper', () => {
    describe('getServiceUrl', () => {
        it('обычный сервис на ru', () => {
            expect(getServiceUrl('www', 'ru')).toEqual('https://yandex.ru');
        });

        it('обычный сервис на com.tr', () => {
            expect(getServiceUrl('www', 'tr')).toEqual('https://yandex.com.tr');
        });

        describe('passport', () => {
            it('passport в проде на ru', () => {
                expect(getServiceUrl('passport', 'ru', 'production')).toEqual('https://passport.yandex.ru');
            });

            it('passport в проде на com.tr', () => {
                expect(getServiceUrl('passport', 'tr', 'production')).toEqual('https://passport.yandex.com.tr');
            });

            it('passport в тестинге на ru', () => {
                expect(getServiceUrl('passport', 'ru', 'testing')).toEqual('https://passport-test.yandex.ru');
            });

            it('passport в тестинге на com.tr', () => {
                expect(getServiceUrl('passport', 'tr', 'testing')).toEqual('https://passport-test.yandex.com.tr');
            });

            it('passport в дэве на ru', () => {
                expect(getServiceUrl('passport', 'ru', 'development')).toEqual('https://passport-test.yandex.ru');
            });

            it('passport в дэве на com.tr', () => {
                expect(getServiceUrl('passport', 'tr', 'development')).toEqual('https://passport-test.yandex.com.tr');
            });
        });

        describe('docviewer', () => {
            it('docviewer в проде на ru', () => {
                expect(getServiceUrl('docviewer', 'ru', 'production')).toEqual('https://docviewer.yandex.ru');
            });

            it('docviewer в проде на com.tr', () => {
                expect(getServiceUrl('docviewer', 'tr', 'production')).toEqual('https://docviewer.yandex.com.tr');
            });

            it('docviewer в тестинге на ru', () => {
                expect(getServiceUrl('docviewer', 'ru', 'testing')).toEqual('https://docviewer.dst.yandex.ru');
            });

            it('docviewer в тестинге на com.tr', () => {
                expect(getServiceUrl('docviewer', 'tr', 'testing')).toEqual('https://docviewer.dst.yandex.com.tr');
            });

            it('docviewer в дэве на ru', () => {
                expect(getServiceUrl('docviewer', 'ru', 'development')).toEqual('https://docviewer.dst.yandex.ru');
            });

            it('docviewer в дэве на com.tr', () => {
                expect(getServiceUrl('docviewer', 'tr', 'development')).toEqual('https://docviewer.dst.yandex.com.tr');
            });
        });

        describe('disk c передачей текущего хоста', () => {
            it('disk из клиента в проде на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'production', 'disk.yandex.ru', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.yandex.ru');
            });

            it('disk из клиента в проде на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'production', 'disk.yandex.com.tr', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.yandex.com.tr');
            });

            it('disk из клиента в престейбле 1 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'prestable', 'disk.dsp.yandex.ru', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.dsp.yandex.ru');
            });

            it('disk из клиента в престейбле 1  на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'prestable', 'disk.dsp.yandex.com.tr', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.dsp.yandex.com.tr');
            });

            it('disk из клиента в престейбле 2 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'prestable', 'disk2.dsp.yandex.ru', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk2.dsp.yandex.ru');
            });

            it('disk из клиента в престейбле 2  на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'prestable', 'disk2.dsp.yandex.com.tr', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk2.dsp.yandex.com.tr');
            });

            it('disk из клиента в тестинге 1 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'testing', 'disk.dst.yandex.ru', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.dst.yandex.ru');
            });

            it('disk из клиента в тестинге 1 на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'testing', 'disk.dst.yandex.com.tr', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk.dst.yandex.com.tr');
            });

            it('disk из клиента в тестинге 2 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'testing', 'disk2.dst.yandex.ru', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk2.dst.yandex.ru');
            });

            it('disk из клиента в тестинге 2 на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'testing', 'disk2.dst.yandex.com.tr', OUR_SERVICES_HASHMAP.CLIENT))
                    .toEqual('https://disk2.dst.yandex.com.tr');
            });

            it('disk из клиента в дэве на ru', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'ru',
                        'development',
                        'user.ufo-trusty.dsd.yandex.ru',
                        OUR_SERVICES_HASHMAP.CLIENT
                    )
                ).toEqual('https://user.ufo-trusty.dsd.yandex.ru');
            });

            it('disk из клиента в дэве на com.tr', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'tr',
                        'development',
                        'user.ufo-trusty.dsd.yandex.com.tr',
                        OUR_SERVICES_HASHMAP.CLIENT
                    )
                ).toEqual('https://user.ufo-trusty.dsd.yandex.com.tr');
            });

            it('disk из паблика в проде на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'production', 'yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.yandex.ru');
            });

            it('disk из паблика в проде на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'production', 'yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.yandex.com.tr');
            });

            it('disk из паблика в престейбле 1 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'prestable', 'public.dsp.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.dsp.yandex.ru');
            });

            it('disk из паблика в престейбле 1  на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'prestable', 'public.dsp.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.dsp.yandex.com.tr');
            });

            it('disk из паблика в престейбле 2 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'prestable', 'public2.dsp.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk2.dsp.yandex.ru');
            });

            it('disk из паблика в престейбле 2  на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'prestable', 'public2.dsp.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk2.dsp.yandex.com.tr');
            });

            it('disk из паблика в тестинге 1 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'testing', 'public.dst.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.dst.yandex.ru');
            });

            it('disk из паблика в тестинге 1 на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'testing', 'public.dst.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk.dst.yandex.com.tr');
            });

            it('disk из паблика в тестинге 2 на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'testing', 'public2.dst.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk2.dst.yandex.ru');
            });

            it('disk из паблика в тестинге 2 на com.tr', () => {
                expect(getServiceUrl('disk', 'tr', 'testing', 'public2.dst.yadi.sk', OUR_SERVICES_HASHMAP.PUBLIC))
                    .toEqual('https://disk2.dst.yandex.com.tr');
            });

            it('disk из паблика в дэве на ru', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'ru',
                        'development',
                        'user.disk-dev.dsd.yadi.sk',
                        OUR_SERVICES_HASHMAP.PUBLIC
                    )
                ).toEqual('https://user.disk-dev.dsd.yandex.ru');
            });

            it('disk из паблика в дэве на com.tr', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'tr',
                        'development',
                        'user.disk-dev.dsd.yadi.sk',
                        OUR_SERVICES_HASHMAP.PUBLIC
                    )
                ).toEqual('https://user.disk-dev.dsd.yandex.com.tr');
            });

            it('disk из docviewer в проде на ru', () => {
                expect(getServiceUrl('disk', 'ru', 'production', 'docviewer.yandex.ru', OUR_SERVICES_HASHMAP.DOCVIEWER))
                    .toEqual('https://disk.yandex.ru');
            });

            it('disk из docviewer в проде на com.tr', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'tr',
                        'production',
                        'docviewer.yandex.com.tr',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.yandex.com.tr');
            });

            it('disk из docviewer в престейбле на ru', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'ru',
                        'prestable',
                        'docviewer.dsp.yandex.ru',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.yandex.ru');
            });

            it('disk из docviewer в престейбле на co.il', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'il',
                        'prestable',
                        'docviewer.dsp.yandex.co.il',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.yandex.co.il');
            });

            it('disk из docviewer в тестинге на ru', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'ru',
                        'testing',
                        'docviewer.dst.yandex.ru',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.dst.yandex.ru');
            });

            it('disk из docviewer в тестинге на com.am', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'am',
                        'testing',
                        'docviewer.dst.yandex.com.am',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.dst.yandex.com.am');
            });

            it('disk из docviewer в дэве на ru', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'ru',
                        'development',
                        'dv-front.dsd.yandex.ru',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.dst.yandex.ru');
            });

            it('disk из docviewer в дэве на fr', () => {
                expect(
                    getServiceUrl(
                        'disk',
                        'fr',
                        'development',
                        'dv-front.dsd.yandex.fr',
                        OUR_SERVICES_HASHMAP.DOCVIEWER
                    )
                ).toEqual('https://disk.dst.yandex.fr');
            });
        });

        describe('yastatic в проде', () => {
            it('yastatic в проде на ru', () => {
                expect(getServiceUrl('yastatic', 'ru', 'production')).toEqual('https://yastatic.net');
            });

            it('yastatic в проде на ru c передачей host', () => {
                expect(getServiceUrl('yastatic', 'ru', 'production', 'disk.yandex.ru')).toEqual('https://yastatic.net');
            });

            it('yastatic в проде на com.tr', () => {
                expect(getServiceUrl('yastatic', 'tr', 'production')).toEqual('https://yastatic.net');
            });

            it('yastatic в престейбле на ru', () => {
                expect(getServiceUrl('yastatic', 'ru', 'prestable')).toEqual('https://yastatic.net');
            });

            it('yastatic в престейбле на ru c передачей host', () => {
                expect(getServiceUrl('yastatic', 'ru', 'prestable', 'disk.dsp.yandex.ru'))
                    .toEqual('https://yastatic.net');
            });

            it('yastatic в престейбле на com.tr', () => {
                expect(getServiceUrl('yastatic', 'tr', 'prestable')).toEqual('https://yastatic.net');
            });

            it('yastatic в тестинге на ru', () => {
                expect(getServiceUrl('yastatic', 'ru', 'testing')).toEqual('https://yastatic.net');
            });

            it('yastatic в тестинге на ru c передачей host', () => {
                expect(getServiceUrl('yastatic', 'ru', 'testing', 'disk.dst.yandex.ru'))
                    .toEqual('https://yastatic.net');
            });

            it('yastatic в тестинге на com.tr', () => {
                expect(getServiceUrl('yastatic', 'tr', 'testing')).toEqual('https://yastatic.net');
            });

            it('yastatic в дэве на ru', () => {
                expect(getServiceUrl('yastatic', 'ru', 'development', 'user.ufo-trusty.dsd.yandex.ru'))
                    .toEqual('https://user.ufo-trusty.dsd.yandex.net');
            });

            it('yastatic в дэве на com.tr', () => {
                expect(getServiceUrl('yastatic', 'tr', 'development', 'user.ufo-trusty.dsd.yandex.com.tr'))
                    .toEqual('https://user.ufo-trusty.dsd.yandex.net');
            });

            it('yastatic в дэве на sk', () => {
                expect(getServiceUrl('yastatic', 'sk', 'development', 'user.ufo-trusty.dsd.yadi.sk'))
                    .toEqual('https://user.ufo-trusty.dsd.yandex.net');
            });

            it('yastatic в дэве на ru', () => {
                expect(getServiceUrl('yastatic', 'ru', 'development', 'user.ufo-trusty.dsd.yandex.ru'))
                    .toEqual('https://user.ufo-trusty.dsd.yandex.net');
            });

            it('yastatic в дэве на com.tr', () => {
                expect(getServiceUrl('yastatic', 'tr', 'development', 'user.ufo-trusty.dsd.yandex.com.tr'))
                    .toEqual('https://user.ufo-trusty.dsd.yandex.net');
            });
        });
    });

    describe('getDownloaderHost', () => {
        it('продовый заберун на ru-домене', () => {
            expect(getDownloaderHost('ru', 'production')).toEqual('downloader.disk.yandex.ru');
        });

        it('продовый заберун на tr-домене', () => {
            expect(getDownloaderHost('tr', 'production')).toEqual('downloader.disk.yandex.com.tr');
        });

        it('продовый заберун на net-домене', () => {
            expect(getDownloaderHost('net', 'production')).toEqual('downloader.disk.yandex.net');
        });

        it('prestable заберун на ru-домене', () => {
            expect(getDownloaderHost('ru', 'prestable')).toEqual('downloader.disk.yandex.ru');
        });

        it('prestable заберун на tr-домене', () => {
            expect(getDownloaderHost('tr', 'prestable')).toEqual('downloader.disk.yandex.com.tr');
        });

        it('prestable заберун на net-домене', () => {
            expect(getDownloaderHost('net', 'prestable')).toEqual('downloader.disk.yandex.net');
        });

        it('testing заберун на ru-домене', () => {
            expect(getDownloaderHost('ru', 'testing')).toEqual('downloader.dst.yandex.ru');
        });

        it('testing заберун на tr-домене', () => {
            expect(getDownloaderHost('tr', 'testing')).toEqual('downloader.dst.yandex.com.tr');
        });

        it('testing заберун на net-домене', () => {
            expect(getDownloaderHost('net', 'testing')).toEqual('downloader.dst.yandex.net');
        });

        it('development заберун на ru-домене', () => {
            expect(getDownloaderHost('ru', 'development')).toEqual('downloader.dst.yandex.ru');
        });

        it('development заберун на tr-домене', () => {
            expect(getDownloaderHost('tr', 'development')).toEqual('downloader.dst.yandex.com.tr');
        });

        it('development заберун на net-домене', () => {
            expect(getDownloaderHost('net', 'development')).toEqual('downloader.dst.yandex.net');
        });
    });
});
