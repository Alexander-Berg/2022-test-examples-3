import nock from 'nock';

import { delay } from './utils/time';
import { Logging } from './utils/Logging';
import { MagicLinks } from './MagicLinks';

function getHostname() {
    const r = Math.random() * Math.random();

    return `magiclinks-${String(r).split('.').pop()}.yandex-team.ru`;
}

function createHref(url: string): HTMLAnchorElement {
    const href = document.createElement('a');

    href.setAttribute('href', url);
    href.appendChild(document.createTextNode('LINK!'));

    return href;
}

describe('MagicLinks', () => {
    it('Should be an instance of MagicLinks', () => {
        expect(new MagicLinks()).toBeInstanceOf(MagicLinks);
    });

    it('Should resolve magic links', async() => {
        const hostname = getHostname();
        const getMagicHrefOf = (href: HTMLAnchorElement): HTMLAnchorElement | null => {
            const magicHref = href.nextElementSibling as HTMLAnchorElement | null;

            if (magicHref && magicHref.classList.contains('MagicLink')) {
                return magicHref;
            }

            return null;
        };

        nock(`https://${hostname}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://href.com/1': {
                        type: 'list',
                        completed: true,
                        ttl: 60,
                        value: [],
                    },
                },
            });

        nock(`https://${hostname}`)
            .defaultReplyHeaders({
                'Access-Control-Allow-Credentials': 'true',
                'Access-Control-Allow-Origin': '*',
            })
            .post('/magiclinks/v1/links/')
            .reply(200, {
                data: {
                    'https://href.com/2': {
                        type: 'list',
                        completed: true,
                        ttl: 60,
                        value: [],
                    },
                },
            });

        const magicLinks = new MagicLinks({
            logging: new Logging(),
            watchTtl: false,
            requestDelay: 0,
            maxBufferTime: 0,
            magicEndpoint: { hostname },
        });

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';

        const href11 = createHref(url1);
        const href12 = createHref(url1);
        const href21 = createHref(url2);

        const ctxElem1 = document.createElement('div');
        const ctxElem2 = document.createElement('div');

        document.body.appendChild(ctxElem1);
        document.body.appendChild(ctxElem2);

        magicLinks
            .addConsumption(ctxElem1)
            .addConsumption(ctxElem2)
            // Это должно быть предусмотрено,
            // ничего не должно глючить в таком кейсе
            .addConsumption(ctxElem1);

        await delay(100);

        // ATTACH CASE
        ctxElem1.appendChild(href11);
        await delay(500);

        ctxElem1.appendChild(href12);
        await delay(500);

        ctxElem2.appendChild(href21);
        await delay(500);

        let magicHref11 = getMagicHrefOf(href11);
        let magicHref12 = getMagicHrefOf(href12);
        let magicHref21 = getMagicHrefOf(href21);

        expect(magicHref11).not.toBe(null);
        expect(magicHref12).not.toBe(null);
        expect(magicHref21).not.toBe(null);

        // UPDATE CASE
        href11.appendChild(document.createTextNode('11'));
        await delay(100);

        href12.appendChild(document.createTextNode('12'));
        await delay(100);

        href21.appendChild(document.createTextNode('21'));
        await delay(100);

        expect(getMagicHrefOf(href11)).not.toBe(null);
        expect(getMagicHrefOf(href12)).not.toBe(null);
        expect(getMagicHrefOf(href21)).not.toBe(null);
        expect(getMagicHrefOf(href11)).not.toBe(magicHref11);
        expect(getMagicHrefOf(href12)).not.toBe(magicHref11);
        expect(getMagicHrefOf(href21)).not.toBe(magicHref21);

        // DETACH CASE
        document.body.removeChild(ctxElem1);

        await delay(100);

        expect(getMagicHrefOf(href11)).toBe(null);
        expect(getMagicHrefOf(href12)).toBe(null);

        // noop
        magicLinks.delConsumption(ctxElem1);

        expect(getMagicHrefOf(href11)).toBe(null);

        magicLinks.delConsumption(ctxElem2);

        expect(getMagicHrefOf(href21)).toBe(null);
    });
});
