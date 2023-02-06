import * as React from 'react';
import 'jest';
import { render, shallow } from 'enzyme';
import { Link } from '../Link';
import { getLinkData } from '../utils/getLinkData';
import { TLinkTarget } from '../Link.types';

const TURBO_IN_TURBO = 'https://yandex.ru/turbo?text=test';
const OUT_URL = 'https://yandex.ru';
const TEL = 'tel:88005553535';
const MAILTO = 'mailto:example@email.com';

const testTargets: TLinkTarget[] = ['_blank', '_self', undefined];

const getUrls = (hosts: string[], paths: string[]) => {
    const container = [...paths];

    ['//', 'http://', 'https://'].forEach(scheme => {
        hosts.forEach(host => {
            paths.forEach(path => {
                container.push(`${scheme}${host}${path}`);
            });
        });
    });

    return container;
};

const inHosts = ['yandex.ru', 'yandex.com.tr', 'werreour-1-ws3.si.yandex.ru', 'renderer-turbo-pull-5756.hamster.yandex.ru'];
const inPaths = [
    '/turbo',
    '/turbo?text=about',
    '/sport',
    '/sport/some/path',
    '/turbo?stub=page%2Fdefault.json',
];

const outHosts = ['google.com', 'rambler.net'];
const outPaths = ['/some-other-service'];

const turboInTurboUrls = getUrls(inHosts, inPaths);
const outTurboLinks = getUrls(outHosts, outPaths);

describe('Реактовая ссылка', () => {
    it('Рендерится без ошибок', () => {
        shallow(<Link text="test" />);
    });

    it('К урлу турбо-в-турбо добавляется reqid', () => {
        const link = render(<Link url={TURBO_IN_TURBO} reqid="somereqid" />);
        expect(link.attr('href').includes('parent-reqid=somereqid')).toEqual(true);
    });

    it('К внешней ссылке добавляется target="_blank"', () => {
        testTargets.forEach(target => {
            const link = render(<Link url={OUT_URL} target={target} />);
            expect(link.attr('target')).toEqual('_blank');
        });
    });

    it('У ссылок начинающихся на tel:/mailto: всегда target="_parent"', () => {
        testTargets.forEach(target => {
            [TEL, MAILTO].forEach(url => {
                const link = render(<Link url={url} target={target} />);
                expect(link.attr('target')).toEqual('_parent');
            });
        });
    });

    describe('Получение данных о ссылке', () => {
        it('Возвращает корректный таргет для урлов на телефон / email', () => {
            testTargets.forEach(target => {
                [TEL, MAILTO].forEach(url => {
                    const { url: newUrl, mix, target: newTarget } = getLinkData({ url, target, reqid: 'somereqid' });
                    expect(mix).toBe(undefined);
                    expect(newUrl).toBe(url);
                    expect(newTarget).toBe('_parent');
                });
            });
        });

        testTargets.forEach(target => {
            turboInTurboUrls.forEach(url => {
                it(`Корректно разбирает урл турбо-в-турбо: ${url}`, () => {
                    const { url: newUrl, mix, target: newTarget } = getLinkData({ url, target, reqid: 'somereqid' });
                    expect(mix).toBe('link-like link-like_type_turbo-navigation-react ym-disable-tracklink');
                    expect(newUrl).toBe(url + `${url.includes('?') ? '&' : '?'}parent-reqid=somereqid`);
                    expect(newTarget).toBe(target || '_self');
                });
            });
        });

        testTargets.forEach(target => {
            outTurboLinks.forEach(url => {
                it(`Корректно разбирает урл вне турбо-в-турбо: ${url}`, () => {
                    const { url: newUrl, mix, target: newTarget } = getLinkData({ url, target, reqid: 'somereqid' });
                    expect(mix).toBe(undefined);
                    expect(newUrl).toBe(url);
                    expect(newTarget).toBe('_blank');
                });
            });
        });
    });

    describe('Проп notModifyTarget', () => {
        it('Неменяет target="_blank", если передан проп notModifyTarget', () => {
            const link = render(<Link url={OUT_URL} target={'_blank'} notModifyTarget />);
            expect(link.attr('target')).toEqual('_blank');
        });

        it('Не устанавливает target, если он не передан и при этом передан проп notModifyTarget', () => {
            const link = render(<Link url={OUT_URL} notModifyTarget />);
            expect(link.attr('target')).toEqual(undefined);
        });
    });
});
