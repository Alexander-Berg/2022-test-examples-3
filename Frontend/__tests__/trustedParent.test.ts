import { YANDEX_TLD } from '@yandex-int/messenger.utils';
import { isOriginAllowed, getTrustedParent } from '../trustedParent';

describe('Trusted parent', () => {
    describe('#isAllowedOrigin', () => {
        it('should allow production', () => {
            YANDEX_TLD.forEach((tld) => expect(isOriginAllowed(`https://yandex.${tld}`)).toBe(true));
        });

        it('should allow www production', () => {
            YANDEX_TLD.forEach((tld) => expect(isOriginAllowed(`https://www.yandex.${tld}`)).toBe(true));
        });

        it('should allow yandex services', () => {
            expect(isOriginAllowed('https://market.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://m.market.yandex.ua')).toBe(true);
        });

        it('should allow local betas', () => {
            expect(isOriginAllowed('https://local.yandex.ru:3443')).toBe(true);
            expect(isOriginAllowed('http://local.yandex.ru:3333')).toBe(true);
            expect(isOriginAllowed('https://local.yandex.ru')).toBe(true);
            expect(isOriginAllowed('http://local.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://localhost.yandex.ru:9001')).toBe(true);
            expect(isOriginAllowed('https://localhost.yandex.ru:8443')).toBe(true);
        });

        it('should allow localhost', () => {
            expect(isOriginAllowed('https://localhost:3443')).toBe(true);
            expect(isOriginAllowed('http://localhost:3333')).toBe(true);
            expect(isOriginAllowed('https://localhost')).toBe(true);
            expect(isOriginAllowed('http://localhost')).toBe(true);
            expect(isOriginAllowed('https://localhost.msup.yandex.ru:8145')).toBe(true);
        });

        it('should allow yandex-team', () => {
            expect(isOriginAllowed('https://st.yandex-team.ru')).toBe(true);
            expect(isOriginAllowed('https://localhost.msup.yandex-team.ru')).toBe(true);
            expect(isOriginAllowed('https://lego-staging.dev.yandex-team.ru')).toBe(true);
            expect(isOriginAllowed('https://styandex-team.ru')).toBe(false);
        });

        it('should allow beru.ru', () => {
            expect(isOriginAllowed('https://beru.ru')).toBe(true);
            expect(isOriginAllowed('https://m.beru.ru')).toBe(true);
            expect(isOriginAllowed('http://dev.pr.beru.ru')).toBe(true);
            expect(isOriginAllowed('https://mberu.ru')).toBe(false);
            expect(isOriginAllowed('https://m.beru.ua')).toBe(false);
        });

        it('should allow eljur', () => {
            expect(isOriginAllowed('https://eljur.ru')).toBe(true);
            expect(isOriginAllowed('https://m.eljur.ru')).toBe(true);
            expect(isOriginAllowed('https://m.eljur.kz')).toBe(true);
            expect(isOriginAllowed('https://m.eljur.kg')).toBe(true);
            expect(isOriginAllowed('https://m.eljur.tr')).toBe(false);
            expect(isOriginAllowed('http://foo.m.eljur.ru')).toBe(true);
            expect(isOriginAllowed('http://foo.m.eljur.com')).toBe(false);
            expect(isOriginAllowed('http://foo.obrcorp.ru')).toBe(true);
            expect(isOriginAllowed('http://foo.obrcorp.com')).toBe(false);
            expect(isOriginAllowed('https://markbook.letovo.ru')).toBe(true);
            expect(isOriginAllowed('https://markbook.letovo.ru')).toBe(true);
            expect(isOriginAllowed('https://eljur.ftl.name')).toBe(true);
            expect(isOriginAllowed('https://cop.admhmao.ru')).toBe(true);
        });

        it('should allow developers eljur', () => {
            expect(isOriginAllowed('https://eljur.my')).toBe(true);
            expect(isOriginAllowed('https://m.eljur.my')).toBe(true);
            expect(isOriginAllowed('http://foo.m.eljur.my')).toBe(true);
            expect(isOriginAllowed('http://foo.m.eljur.com')).toBe(false);
            expect(isOriginAllowed('http://t1.eljur.tech')).toBe(true);
            expect(isOriginAllowed('http://foo.t1.eljur.tech')).toBe(true);
            expect(isOriginAllowed('http://foo.t5.eljur.tech')).toBe(true);
            expect(isOriginAllowed('http://foo.t10.eljur.tech')).toBe(true);
        });

        it('should allow widget stand', () => {
            expect(isOriginAllowed('https://messenger-test.s3.mds.yandex.net')).toBe(true);
            expect(isOriginAllowed('https://messenger-testls3lmdslyandexlnet')).toBe(false);
        });

        it('should allow public beta', () => {
            expect(isOriginAllowed('https://sivashev-1-ci1.si.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://olliva-2-ci1.si.yandex.ua')).toBe(true);
            expect(isOriginAllowed('https://a-lexx-1-ci3.si.yandex.by')).toBe(true);
            expect(isOriginAllowed('https://ivan123-123-ci423.si.yandex.kz')).toBe(true);
            expect(isOriginAllowed('https://ivan-123-321-ci321.si.yandex.com')).toBe(true);
            expect(isOriginAllowed('https://test-0-ci0.si.yandex.com.tr')).toBe(true);

            expect(isOriginAllowed('https://templates.priemka.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://renderer-web4-dev.hamster.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://renderer-web4-pull-1.hamster.yandex.ru')).toBe(true);
        });

        it('should decline empty string', () => {
            // undefined and not a string validate by TypeScript
            expect(isOriginAllowed('')).toBe(false);
            expect(isOriginAllowed(' ')).toBe(false);
            expect(isOriginAllowed('            ')).toBe(false);
        });

        it('should decline random', () => {
            expect(isOriginAllowed('adjdf 834f 8qf8q h39 rh83 gh83')).toBe(false);
            expect(isOriginAllowed('000000000000')).toBe(false);
        });

        it('should decline fake yandex hosts', () => {
            expect(isOriginAllowed('https://yandex.ru.com')).toBe(false);
            expect(isOriginAllowed('https://yandex.nl')).toBe(false);
            expect(isOriginAllowed('https://myyandex.ru')).toBe(false);
        });

        it('should decline not secure yandex hosts', () => {
            expect(isOriginAllowed('http://yandex.ru')).toBe(false);
        });

        it('turbo sites', () => {
            expect(isOriginAllowed('https://project591405.turbo.site')).toBe(true);
            expect(isOriginAllowed('https://turbo.site')).toBe(true);
            expect(isOriginAllowed('https://foo.turbo.site')).toBe(true);
            expect(isOriginAllowed('https://foo.turbo-site-develop.common.yandex.net')).toBe(true);
            expect(isOriginAllowed('https://foo.turbo-site-test.common.yandex.net')).toBe(true);
            expect(isOriginAllowed('https://foo.turbo-site-staging.common.yandex.net')).toBe(true);
            expect(isOriginAllowed('https://foo.lc-internal.yandex.net')).toBe(true);
            expect(isOriginAllowed('https://localhost.msup.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://localhost.msup.yandex.ru:8082')).toBe(true);
            expect(isOriginAllowed('https://foo.localhost.msup.yandex.ru')).toBe(true);
            expect(isOriginAllowed('https://foo.localhost.msup.yandex.ru:8082')).toBe(true);
        });

        it('zen', () => {
            expect(isOriginAllowed('https://foo-1-bar.kaizen.yandex.ru')).toBeTruthy();
            expect(isOriginAllowed('https://foo_2-bar.zen.zeta.kaizen.yandex.ru')).toBeTruthy();
            expect(isOriginAllowed('https://foo_3-bar.zdevx.yandex.ru')).toBeTruthy();
            expect(isOriginAllowed('https://foo_4-bar.zen-comments.yandex.ru:3400')).toBeTruthy();
            expect(isOriginAllowed('https://local.zdevx.yandex.ru:8080')).toBeTruthy();
            expect(isOriginAllowed('https://local.foo_5-bar.zdevx.yandex.ru')).toBeTruthy();
            expect(isOriginAllowed('https://local.foo_6-bar.zdevx.yandex.ru:9000')).toBeTruthy();
            expect(isOriginAllowed('https://foo_7-bar.crowdzeta.kaizen.yandex.ru')).toBeTruthy();

            // Кейсы с https://wiki.yandex-team.ru/users/arkazantseva/Vse-domeny-Dzena/
            const subdomains = ['br', 'mx', 'uk', 'in', 'us', 'de', 'fr', 'au', 'ph', 'pl', 'pk',
                'es', 'vn', 'id', 'it', 'nl', 'ca', 'cl', 'eg', 'ar', 'th', 'co', 'my', 'sa',
                'ma', 've', 'pe', 'dz', 'at', 'az', 'bd', 'be', 'ba', 'bg', 'cz', 'dk', 'do',
                'ec', 'ge', 'hk', 'hu', 'iq', 'il', 'pt', 'ro', 'rs', 'sg', 'sk', 'lk', 'ch',
                'za', 'hr', 'jp', 'ae', 'kr', 'gr', 'fi', 'se', 'no', 'africa', 'latin'];
            const tlds = ['ru', 'ua', 'kz', 'by', 'kg', 'lt', 'lv', 'md', 'tj', 'tm', 'uz', 'ee',
                'az', 'fr', 'com', 'com.tr', 'com.am', 'com.ge', 'co.il'];

            subdomains.forEach((subdomain) => {
                expect(isOriginAllowed(`https://${subdomain}.zen.yandex.com`)).toBeTruthy();
            });

            tlds.forEach((tld) => {
                expect(isOriginAllowed(`https://zen.yandex.${tld}`)).toBeTruthy();
            });
        });

        it('kinopoisk', () => {
            expect(isOriginAllowed('https://kinopoisk.ru')).toBe(true);
            expect(isOriginAllowed('https://kinopoisk.ru:80420')).toBe(true);
            expect(isOriginAllowed('https://hd.kinopoisk.ru')).toBe(true);
            expect(isOriginAllowed('https://hd.kinopoisk.ru:80420')).toBe(true);
        });

        it('cloud', () => {
            [
                'https://cloud-preprod.yandex.ru',
                'https://scale-preprod.yandex.ru',
                'https://cloud.yandex.ru',
                'https://scale.yandex.ru',
                'https://cloud-preprod.yandex.com',
                'https://scale-preprod.yandex.com',
                'https://cloud.yandex.com',
                'https://scale.yandex.com',
            ].forEach((cloudOrigin) => {
                expect(isOriginAllowed(cloudOrigin)).toBeTruthy();
            });
        });

        it('metrika', () => {
            [
                'https://metrika.yandex.ru',
                'https://ps.metrika.yandex.ru',
                'https://test.metrika.yandex.ru',
                'https://something.dev.metrika.yandex.ru',
                'https://metrica.yandex.ru',
                'https://ps.metrica.yandex.ru',
                'https://test.metrica.yandex.ru',
                'https://something.dev.metrica.yandex.ru',
            ].forEach((metrikaOrigin) => {
                expect(isOriginAllowed(metrikaOrigin)).toBeTruthy();
            });
        });

        it('travel', () => {
            [
                'https://travel.yandex.ru',
                'https://travel-prestable.yandex.ru',
                'https://travel-test.yandex.ru',
                'https://travel-unstable.yandex.ru',
                'https://something.travel.farm.yandex.ru',
                'https://something.travel.ui.yandex.ru ',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('toloka', () => {
            [
                'https://toloka.ai',
                'https://we.toloka.ai',
                'https://toloka.yandex.ru',
                'https://toloka.yandex.com',
                'https://test.toloka-test.ai',
                'https://test.toloka-test.ai:9001',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('webmaster', () => {
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
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('plus', () => {
            [
                'https://local.plus-test.yandex.ru',
                'https://promo-stage-цифра.pr.plus.tst.yandex.ru',
                'https://rc.plus.tst.yandex.ru/',
                'https://plus.tst.yandex.ru/',
                'https://stage-rc.plus.tst.yandex.ru/',
                'https://plus.prestable.yandex.ru/',
                'https://plus.yandex.ru/',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('partner', () => {
            [
                'https://partner2.yandex.ru',
                'https://partner2.yandex.com',
                'https://partner.yandex.ru',
                'https://partner.yandex.com',
                'https://partners.yandex.ru',
                'https://partners.yandex.com',
                'https://partners.yandex.by',
                'https://partners.yandex.ua',
                'https://partners.yandex.kz,',
                'https://partner2-test.yandex.ru',
                'https://partner2-test.yandex.com',
                'https://test-test.partner.yandex.ru:8094',
                'https://test-test.ui-dev.partner.yandex.ru:8032',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('comedyclub', () => {
            [
                'https://sdoc.comedyclub.ru:444',
                'https://chat.reviews.comedyclub.ru',
                'https://sdoc.wip',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });

            [
                'https://comedyclub.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeFalsy();
            });
        });

        it('delivery', () => {
            [
                'https://logistics-frontend.taxi.tst.yandex.ru',
                'https://logistics-frontend.taxi.dev.yandex.ru',
                'https://dostavka.yandex.ru',
                'https://logistics-frontend.taxi.tst.yandex.com',
                'https://logistics-frontend.yango.tst.yandex.com',
                'https://delivery.yango.com',
                'https://delivery.yango.yandex.com',
                'https://logistics-frontend.taxi.tst.yandex.net',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('eda', () => {
            [
                'https://eda.yandex.ru',
                'https://eda.yandex.kz',
                'https://eda.yandex.com',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('pay', () => {
            expect(isOriginAllowed('https://pay.yandex.ru')).toBeTruthy();
        });

        it('help', () => {
            expect(isOriginAllowed('https://help.yandex.ru')).toBeTruthy();
        });

        it('contest', () => {
            expect(isOriginAllowed('https://contest.yandex.ru')).toBeTruthy();
            expect(isOriginAllowed('https://contest.test.yandex.ru')).toBeTruthy();
        });

        it('taxi', () => {
            [
                'https://business.taxi.yandex.ru',
                'https://corp-client.taxi.dev.yandex.ru',
                'https://corp-client.taxi.tst.yandex.ru',
                'https://corp-client.username.front.taxi.dev.yandex.ru',
                'https://corp-client.username.front.taxi.dev.yandex.ru:8443',
                'https://business.taxi.yandex.by',
                'https://corp-client.taxi.dev.yandex.by',
                'https://corp-client.taxi.tst.yandex.by',
                'https://business.taxi.yandex.com',
                'https://business.yango.yandex.com',
                'https://corp-client.taxi.dev.yandex.com',
                'https://corp-client.taxi.tst.yandex.com',
                'https://business.yango.taxi.dev.yandex.com',
                'https://business.yango.taxi.tst.yandex.com',
                'https://business.taxi.yandex.kz',
                'https://corp-client.taxi.dev.yandex.kz',
                'https://corp-client.taxi.tst.yandex.kz',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('afisha', () => {
            [
                'http://afisha.yandex.ru/',
                'http://afisha.tst.yandex.ru/',
                'https://afisha-junk-www-stage-1.afisha.tst.yandex.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('q', () => {
            [
                'https://master.answers.yandex.net',
                'https://answers.crowdtest.yandex.ru',
                'https://answers-trunk.yandex.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('surveys', () => {
            [
                'https://surveys.yandex.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('adfox', () => {
            [
                'http://adfox.yandex.ru/',
                'http://adfox.yandex.com/',
                'http://adfox.ui-dev.adfox.yandex.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('kolhoz', () => {
            [
                'https://kolhoz.yandex-team.ru',
                'https://kolhoz-test-assessors.yandex-team.ru',
                'https://sandbox.kolhoz.yandex-team.ru',
                'https://kolhoz-test.yandex-team.ru',
                'https://localhost.yandex-team.ru:8081',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('farm', () => {
            [
                'https://farm.yandex.ru',
                'https://test.farm.yandex.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });

        it('quantum', () => {
            [
                'https://quantum.yandex-team.ru',
                'https://test.quantum.yandex-team.ru',
                'https://prestable.quantum.yandex-team.ru',
                'https://betatest.quantum.yandex-team.ru',
                'https://pr-1.beta.frontend.lms.yandex-team.ru',
                'https://lms-local.yandex-team.ru',
            ].forEach((origin) => {
                expect(isOriginAllowed(origin)).toBeTruthy();
            });
        });
    });

    describe('#getTrustedParent', () => {
        it('empty', () => {
            expect(getTrustedParent('')).toEqual('');
        });

        it('left site', () => {
            expect(getTrustedParent('https://foo.bar')).toEqual('');
        });

        it('right site', () => {
            expect(getTrustedParent('https://yandex.ru')).toEqual('https://yandex.ru');
            expect(getTrustedParent('https://staff.yandex-team.ru')).toEqual('https://staff.yandex-team.ru');
        });
    });
});
