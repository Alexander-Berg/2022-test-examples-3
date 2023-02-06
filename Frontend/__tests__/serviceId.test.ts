import { YANDEX_TLD } from '@yandex-int/messenger.utils';
import * as faker from 'faker';

import { serviceIds } from '../../../../configs/service-ids';
import { guessServiceIdByData, guessServiceIdByOrigin } from '../serviceId';

const buildsNoYabro = ['yamb', 'yamb-internal', 'chamb', 'embed-internal', 'widget'];

function fakeOrigin(config: { domainWord?: string; tld?: string; path?: string; query?: string; } = {}) {
    const {
        domainWord,
        tld,
        path,
        query,
    } = config;

    const acc = [
        `${faker.internet.protocol()}://`,
        domainWord ? domainWord : faker.internet.domainWord(),
        `.${tld ? tld : fakeTld()}`,
    ];

    path && acc.push(path);
    query && acc.push(`?${query}=${faker.random.alphaNumeric()}`);

    return acc.join('');
}

function fakeTld() {
    return faker.internet.domainSuffix();
}

function fakeServiceId() {
    return faker.random.arrayElement(Object.values(serviceIds).filter((id) => id > 0));
}

function fakeNoYabroBuild() {
    return buildsNoYabro[faker.random.number({ min: 0, max: buildsNoYabro.length })];
}

describe('guessServiceIdByData', () => {
    it('should return correct DESKTOP_YABRO for desktop yabro build', () => {
        expect(guessServiceIdByData(
            fakeOrigin(),
            fakeTld(),
            fakeServiceId(),
            'yabro',
        )).toBe(serviceIds.DESKTOP_YABRO);
    });

    it('should return correct serviceId if correct serviceId is provided', () => {
        const serviceId = fakeServiceId();
        expect(guessServiceIdByData(
            fakeOrigin(),
            fakeTld(),
            serviceId,
            fakeNoYabroBuild(),
        )).toBe(serviceId);
    });

    it('should throw Undefined serviceId error if serviceId < 0 is provided', () => {
        expect(() => guessServiceIdByData(
            fakeOrigin(),
            fakeTld(),
            -1,
            fakeNoYabroBuild(),
        )).toThrowError('Undefined serviceId -1');
    });

    it('should throw Undefined serviceId error if serviceId > max value is provided', () => {
        const maxServiceId = Object.values(serviceIds).sort((a, b) => b - a)[0];

        expect(() => guessServiceIdByData(
            fakeOrigin(),
            fakeTld(),
            maxServiceId + 1,
            fakeNoYabroBuild(),
        )).toThrowError(`Undefined serviceId ${maxServiceId + 1}`);
    });

    it('should return WEB for random host', () => {
        expect(guessServiceIdByData(
            fakeOrigin(),
            fakeTld(),
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.WEB);
    });

    it('should return SERVICES id for yandex host with /uslugi/ path', () => {
        const tld = fakeTld();
        const origin = fakeOrigin({
            domainWord: 'yandex',
            tld,
            path: '/uslugi/',
        });

        expect(guessServiceIdByData(
            origin,
            tld,
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.SERVICES);
    });

    it('should return SERP id for "yandex.{tld}/search/"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'yandex',
                tld,
                path: '/search/',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.SERP);
        });
    });

    it('should return LOCAL for "yandex.{tld}/local/"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'yandex',
                tld,
                path: '/local/',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.LOCAL);
        });
    });

    it('should return ETHER for "yandex.{tld}/?stream_channel=XXX"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'yandex',
                tld,
                query: 'stream_channel',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.ETHER);
        });
    });

    it('should return MAP id for "yandex.{tld}/maps/"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'yandex',
                tld,
                path: '/maps/',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.MAP);
        });
    });

    it('should return MARKET for "market.yandex.{tld}"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'market.yandex',
                tld,
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.MARKET);
        });
    });

    it('should return TRAVEL for travel domains', () => {
        YANDEX_TLD.forEach((tld) => {
            [
                'travel',
                'travel-prestable',
                'travel-test',
                'travel-unstable',
                'travel.farm',
                'travel.ui',
            ].forEach((item) => {
                const origin = fakeOrigin({
                    domainWord: `${item}.yandex`,
                    tld,
                });

                expect(guessServiceIdByData(
                    origin,
                    tld,
                    undefined,
                    fakeNoYabroBuild(),
                )).toBe(serviceIds.TRAVEL);
            });
        });
    });

    it('should return MORDA for "yandex.{tld}/"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'yandex',
                tld,
                path: '/',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.MORDA);
        });
    });

    it('should return MORDA  for "www.yandex.{tld}/"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'www.yandex',
                tld,
                path: '/',
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.MORDA);
        });
    });

    it('should return HEALTH for "med.yandex.{tld}"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'med.yandex',
                tld,
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.HEALTH);
        });
    });

    it('should return HEALTH for "health.yandex.{tld}"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'health.yandex',
                tld,
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.HEALTH);
        });
    });

    it('should return BERU for "beru.ru"', () => {
        const tld = 'ru';
        const origin = fakeOrigin({
            domainWord: 'beru',
            tld,
        });

        expect(guessServiceIdByData(
            origin,
            tld,
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.BERU);
    });

    it('should throw error if provided a wrong data', () => {
        expect(() => guessServiceIdByData(
            1 as any,
            fakeTld(),
            undefined,
            fakeNoYabroBuild(),
        )).toThrowError('argument must be of type string');
    });

    it('should return ELJUR for "eljur.ru"', () => {
        const tld = 'ru';
        const origin = fakeOrigin({
            domainWord: 'eljur',
            tld,
        });

        expect(guessServiceIdByData(
            origin,
            tld,
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.ELJUR);
    });

    it('should return ELJUR for "demo.eljur.my"', () => {
        const tld = 'my';
        const origin = fakeOrigin({
            domainWord: 'demo.eljur',
            tld,
        });

        expect(guessServiceIdByData(
            origin,
            tld,
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.ELJUR);
    });

    it('should return TELEMOST for "telemost.yandex.ru"', () => {
        YANDEX_TLD.forEach((tld) => {
            const origin = fakeOrigin({
                domainWord: 'telemost.yandex',
                tld,
            });

            expect(guessServiceIdByData(
                origin,
                tld,
                undefined,
                fakeNoYabroBuild(),
            )).toBe(serviceIds.TELEMOST);
        });
    });

    it('should return TELEMOST for "telemost.yandex-team.ru"', () => {
        const tld = 'ru';
        const origin = fakeOrigin({
            domainWord: 'telemost.yandex-team',
            tld,
        });

        expect(guessServiceIdByData(
            origin,
            tld,
            undefined,
            fakeNoYabroBuild(),
        )).toBe(serviceIds.TELEMOST);
    });

    it('should return CLOUD for clouds domains', () => {
        [
            'https://cloud-preprod.yandex.ru',
            'https://scale-preprod.yandex.ru',
            'https://cloud.yandex.ru',
            'https://scale.yandex.ru',
        ].forEach((cloudOrigin) => {
            expect(guessServiceIdByOrigin(
                cloudOrigin,
                'ru',
            )).toBe(serviceIds.CLOUD);
        });

        [
            'https://cloud-preprod.yandex.com',
            'https://scale-preprod.yandex.com',
            'https://cloud.yandex.com',
            'https://scale.yandex.com',
        ].forEach((cloudOrigin) => {
            expect(guessServiceIdByOrigin(
                cloudOrigin,
                'com',
            )).toBe(serviceIds.CLOUD);
        });
    });

    it('should return METRIKA for metrika domains', () => {
        [
            'https://metrika.yandex.ru',
            'https://ps.metrika.yandex.ru',
            'https://test.metrika.yandex.ru',
            'https://something.dev.metrika.yandex.ru',
            'https://metrica.yandex.ru',
            'https://ps.metrica.yandex.ru',
            'https://test.metrica.yandex.ru',
            'https://something.dev.metrica.yandex.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.METRIKA);
        });

        [
            'https://metrika.yandex.com',
            'https://ps.metrika.yandex.com',
            'https://test.metrika.yandex.com',
            'https://something.dev.metrika.yandex.com',
            'https://metrica.yandex.com',
            'https://ps.metrica.yandex.com',
            'https://test.metrica.yandex.com',
            'https://something.dev.metrica.yandex.com',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'com',
            )).toBe(serviceIds.METRIKA);
        });
    });

    it('should return TOLOKA for toloka domains', () => {
        [
            'https://toloka.ai',
            'https://we.toloka.ai',
            'https://toloka.yandex.ru',
            'https://toloka.yandex.com',
            'https://test.toloka-test.ai',
            'https://test.toloka-test.ai:9001',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.TOLOKA);
        });
    });

    it('should return PLUS for plus domains', () => {
        [
            'https://local.plus-test.yandex.ru',
            'https://promo-stage-цифра.pr.plus.tst.yandex.ru',
            'https://rc.plus.tst.yandex.ru/',
            'https://plus.tst.yandex.ru/',
            'https://stage-rc.plus.tst.yandex.ru/',
            'https://plus.prestable.yandex.ru/',
            'https://plus.yandex.ru/',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.PLUS);
        });
    });

    it('should return COMEDY for comedy domains', () => {
        [
            'https://sdoc.comedyclub.ru:444',
            'https://chat.reviews.comedyclub.ru',
            'https://sdoc.wip',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.COMEDY);
        });

        [
            'https://comedyclub.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.WEB);
        });
    });

    it('should return PARTNER for partner domains', () => {
        [
            'https://partner2.yandex.ru',
            'https://partner.yandex.ru',
            'https://partners.yandex.ru',
            'https://partner2-test.yandex.ru',
            'https://test-test.partner.yandex.ru:8094',
            'https://test-test.ui-dev.partner.yandex.ru:8032',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.PARTNER);
        });

        [
            'https://partner2.yandex.com',
            'https://partner.yandex.com',
            'https://partners.yandex.com',
            'https://partner2-test.yandex.com',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'com',
            )).toBe(serviceIds.PARTNER);
        });

        expect(guessServiceIdByOrigin(
            'https://partners.yandex.by',
            'by',
        )).toBe(serviceIds.PARTNER);

        expect(guessServiceIdByOrigin(
            'https://partners.yandex.ua',
            'ua',
        )).toBe(serviceIds.PARTNER);

        expect(guessServiceIdByOrigin(
            'https://partners.yandex.kz',
            'kz',
        )).toBe(serviceIds.PARTNER);
    });

    it('should return WEBMASTER for webmaster domains', () => {
        [
            'https://webmaster.yandex.ru',
            'https://webmaster.prestable.yandex.ru',
            'https://webmaster.test.yandex.ru',
            'https://webmaster.beta.yandex.ru',
            'https://webmaster-admin.yandex-team.ru',
            'https://webmaster-admin.test.yandex-team.ru',
            'https://trusty-iva.webmaster.dev.yandex.ru',
            'https://trusty-sas.webmaster.dev.yandex.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.WEBMASTER);
        });
    });

    it('should return DELIVERY for webmaster domains', () => {
        [
            'https://logistics-frontend.taxi.tst.yandex.ru',
            'https://logistics-frontend.taxi.dev.yandex.ru',
            'https://dostavka.yandex.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.DELIVERY);
        });

        [
            'https://logistics-frontend.taxi.tst.yandex.com',
            'https://logistics-frontend.yango.tst.yandex.com',
            'https://delivery.yango.com',
            'https://delivery.yango.yandex.com',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'com',
            )).toBe(serviceIds.DELIVERY);
        });

        expect(guessServiceIdByOrigin(
            'https://logistics-frontend.taxi.tst.yandex.net',
            'net',
        )).toBe(serviceIds.DELIVERY);
    });

    it('should return EDA', () => {
        expect(guessServiceIdByOrigin(
            'https://eda.yandex.ru',
            'ru',
        )).toBe(serviceIds.EDA);

        expect(guessServiceIdByOrigin(
            'https://eda.yandex.com',
            'com',
        )).toBe(serviceIds.EDA);

        expect(guessServiceIdByOrigin(
            'https://eda.yandex.kz',
            'by',
        )).toBe(serviceIds.EDA);
    });

    it('should return Pay', () => {
        expect(guessServiceIdByOrigin(
            'https://pay.yandex.ru',
            'ru',
        )).toBe(serviceIds.PAY);
    });

    it('should return Help', () => {
        expect(guessServiceIdByOrigin(
            'https://help.yandex.ru',
            'ru',
        )).toBe(serviceIds.HELP);
    });

    it('should return Contest', () => {
        expect(guessServiceIdByOrigin(
            'https://contest.yandex.ru',
            'ru',
        )).toBe(serviceIds.CONTEST);

        expect(guessServiceIdByOrigin(
            'https://contest.test.yandex.ru',
            'ru',
        )).toBe(serviceIds.CONTEST);
    });

    it('should return Taxi', () => {
        [
            'https://business.taxi.yandex.ru',
            'https://corp-client.taxi.dev.yandex.ru',
            'https://corp-client.taxi.tst.yandex.ru',
            'https://corp-client.username.front.taxi.dev.yandex.ru',
            'https://corp-client.username.front.taxi.dev.yandex.ru:8443',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.TAXI);
        });

        [
            'https://business.taxi.yandex.by',
            'https://corp-client.taxi.dev.yandex.by',
            'https://corp-client.taxi.tst.yandex.by',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'by',
            )).toBe(serviceIds.TAXI);
        });

        [
            'https://business.taxi.yandex.com',
            'https://business.yango.yandex.com',
            'https://corp-client.taxi.dev.yandex.com',
            'https://corp-client.taxi.tst.yandex.com',
            'https://business.yango.taxi.dev.yandex.com',
            'https://business.yango.taxi.tst.yandex.com',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'com',
            )).toBe(serviceIds.TAXI);
        });

        [
            'https://business.taxi.yandex.kz',
            'https://corp-client.taxi.dev.yandex.kz',
            'https://corp-client.taxi.tst.yandex.kz',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'kz',
            )).toBe(serviceIds.TAXI);
        });
    });

    it('should return Afisha', () => {
        [
            'http://afisha.yandex.ru/',
            'http://afisha.tst.yandex.ru/',
            'https://afisha-junk-www-stage-1.afisha.tst.yandex.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.AFISHA);
        });
    });

    it('should return Q', () => {
        [
            'https://yandex.ru/q/',
            'https://l7test.yandex.ru/q',
            'https://master.answers.yandex.net',
            'https://answers.crowdtest.yandex.ru/q',
            'https://answers-trunk.yandex.ru/q',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.Q);
        });
    });

    it('should return Surveys', () => {
        [
            'https://surveys.yandex.ru/',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.SURVEYS);
        });
    });

    it('should return Adfox', () => {
        [
            'https://adfox.yandex.ru/',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.ADFOX);
        });

        [
            'https://adfox.yandex.com/',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'com',
            )).toBe(serviceIds.ADFOX);
        });
    });

    it('should return Kolhoz', () => {
        [
            'https://kolhoz.yandex-team.ru',
            'https://kolhoz-test-assessors.yandex-team.ru',
            'https://sandbox.kolhoz.yandex-team.ru',
            'https://kolhoz-test.yandex-team.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.KOLHOZ);
        });
    });

    it('should return Farm', () => {
        [
            'https://farm.yandex.ru',
            'https://test.farm.yandex.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.FARM);
        });
    });

    it('should retuen Quantum', () => {
        [
            'https://quantum.yandex-team.ru',
            'https://test.quantum.yandex-team.ru',
            'https://prestable.quantum.yandex-team.ru',
            'https://betatest.quantum.yandex-team.ru',
            'https://pr-1.beta.frontend.lms.yandex-team.ru',
            'https://lms-local.yandex-team.ru',
        ].forEach((origin) => {
            expect(guessServiceIdByOrigin(
                origin,
                'ru',
            )).toBe(serviceIds.QUANTUM);
        });
    });
});
