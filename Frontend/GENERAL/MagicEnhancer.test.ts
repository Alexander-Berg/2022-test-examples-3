import { IMagicNode, IMagicUser } from './types/IMagicLink';
import { MagicEnhancer } from './MagicEnhancer';

function createHref(url: string): HTMLAnchorElement {
    const href = document.createElement('a');

    href.setAttribute('href', url);
    href.appendChild(document.createTextNode(url));

    return href;
}

describe('MagicEnhancer', () => {
    it('Should be an instance of MagicEnhancer', () => {
        const enhancer = new MagicEnhancer();

        expect(enhancer).toBeInstanceOf(MagicEnhancer);
    });

    it('Should make hrefs magic and ordinary back', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [],
        });

        expect(href.classList.contains('MagicLink-Hidden')).toBe(true);

        const magicHref0 = href.nextElementSibling as HTMLAnchorElement;

        expect(magicHref0.parentNode).not.toBeNull();

        expect(magicHref0).not.toBeNull();
        expect(magicHref0.classList.contains('MagicLink'));

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [],
        });

        const magicHref = href.nextElementSibling as HTMLAnchorElement;

        expect(magicHref).not.toBeNull();
        expect(magicHref0).not.toBe(magicHref);
        expect(magicHref0.parentNode).toBeNull();
        expect(magicHref.parentNode).not.toBeNull();

        enhancer.reverse(href);

        expect(href.classList.contains('MagicLink-Hidden')).toBe(false);
        expect(href.nextElementSibling).toBeNull();
        expect(ctxElem.querySelector('.MagicLink')).toBeNull();

        expect(() => {
            enhancer.reverse(href);
        }).not.toThrow();
    });

    it('Should support MagicImage', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'image',
                    src: 'test',
                },
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        expect(magicHref.classList.contains('MagicLink'));
        expect(magicHref.querySelector('.MagicLink-Image')).not.toBeNull();
    });

    it('Should support MagicString', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'string',
                    value: 'test',
                },
            ],
        });
        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        expect(magicHref.classList.contains('MagicLink'));
        expect(magicHref.querySelector('.MagicLink-String')).not.toBeNull();
    });

    it('Should support MagicString with color', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'string',
                    value: 'test',
                    color: 'red',
                },
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        expect(magicHref.classList.contains('MagicLink'));
        expect(magicHref.querySelector('.MagicLink-String[style="color: red;"]')).not.toBeNull();
    });

    it('Should support MagicString with strike', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'string',
                    value: 'test',
                    strike: true,
                },
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        expect(magicHref.classList.contains('MagicLink'));
        expect(magicHref.querySelector('.MagicLink-String.MagicLink-Strike')).not.toBeNull();
    });

    it('Should disable Preview', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer({ disablePreview: true });

        const url = 'https://href.com/2';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: false,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'string',
                    value: 'test1',
                    action: {
                        event: 'click',
                        type: 'halfscreenpreview',
                        url: 'test',
                    },
                },
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        const action = magicHref.querySelector('.MagicLink-String.MagicLink-Action') as HTMLElement;

        expect(action).toBeNull();

        expect(document.querySelector('.MagicLink-ActionCard')).toBeNull();
    });

    it('Should support MagicString with action', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();
        const url = 'https://href.com/1';
        const href = createHref(url);
        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'string',
                    value: 'test1',
                    action: {
                        event: 'click',
                        type: 'halfscreenpreview',
                        url: 'test',
                    },
                },
                {
                    type: 'string',
                    value: 'test2',
                    action: {
                        event: 'click',
                        type: 'halfscreenpreview',
                        url: 'test',
                    },
                },
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        const visible = () => {
            expect(document.querySelector('.MagicLink-ActionCard:not(.MagicLink-ActionCard_hidden)')).not.toBeNull();
        };

        const invisible = () => {
            expect(document.querySelector('.MagicLink-ActionCard.MagicLink-ActionCard_hidden')).not.toBeNull();
        };

        const string = magicHref.querySelector('.MagicLink-String.MagicLink-Action') as HTMLElement;

        string.dispatchEvent(new MouseEvent('click', {
            metaKey: true,
        }));

        invisible();

        string.dispatchEvent(new MouseEvent('click'));

        visible();

        document.dispatchEvent(new MouseEvent('click'));

        invisible();

        string.dispatchEvent(new MouseEvent('click'));

        visible();

        const close = document.querySelector('.MagicLink-ActionCard .MagicLink-ActionCardClose') as HTMLElement;

        close.dispatchEvent(new MouseEvent('click'));

        invisible();

        string.dispatchEvent(new MouseEvent('click'));

        visible();

        string.dispatchEvent(new MouseEvent('click'));

        invisible();

        string.dispatchEvent(new MouseEvent('click'));

        visible();

        const string2 = magicHref.querySelector('.MagicLink-String.MagicLink-Action:last-child') as HTMLElement;

        string2.dispatchEvent(new MouseEvent('click'));

        visible();
    });

    it('Should support MagicUser', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer({
            staffEndpoint: {
                protocol: 'https:',
                hostname: 'staff-x.yandex-team.ru',
            },
        });

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'user',
                    login: 'x1',
                    value: {
                        name: '1Name',
                        lastName: '2Last',
                        isDismissed: true,
                    },
                } as IMagicUser,
                {
                    type: 'user',
                    login: 'x2',
                    value: {
                        name: '1Name',
                        lastName: '',
                    },
                } as IMagicUser,
                {
                    type: 'user',
                    login: 'x3',
                } as IMagicUser,
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        function getFirstLetterText(login: string): string {
            const firstLetter = magicHref.querySelector(`.MagicLink-User[href*="https://staff-x.yandex-team.ru/${login}"] .MagicLink-UserFirstLetter`) as HTMLElement;

            return firstLetter.innerHTML;
        }

        expect(magicHref.querySelector('.MagicLink-User[data-staff="x1"]')).not.toBeNull();
        expect(magicHref.querySelector('.MagicLink-User[data-staff="x2"]')).not.toBeNull();
        expect(magicHref.querySelector('.MagicLink-User[data-staff="x3"]')).not.toBeNull();

        expect(magicHref.querySelector('.MagicLink-User[href*="https://staff-x.yandex-team.ru/x1"].MagicLink-User_dismissed')).not.toBeNull();
        expect(getFirstLetterText('x1')).toStrictEqual('1');
        expect(getFirstLetterText('x2')).toStrictEqual('1');
        expect(getFirstLetterText('x3')).toStrictEqual('x');
    });

    it('Should support not MagicUser', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer({
            staffEndpoint: false,
        });

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                {
                    type: 'user',
                    login: 'x1',
                    value: {
                        name: '1Name',
                        lastName: '2Last',
                        isDismissed: true,
                    },
                } as IMagicUser,
                {
                    type: 'user',
                    login: 'x2',
                    value: {
                        name: '1Name',
                        lastName: '',
                    },
                } as IMagicUser,
                {
                    type: 'user',
                    login: 'x3',
                } as IMagicUser,
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        function getFirstLetterText(login: string): string {
            const firstLetter = magicHref.querySelector(`.MagicLink-User[data-staff="${login}"] .MagicLink-UserFirstLetter`) as HTMLElement;

            return firstLetter.innerHTML;
        }

        expect(magicHref.querySelector('.MagicLink-User[data-staff="x1"]')).not.toBeNull();
        expect(magicHref.querySelector('.MagicLink-User[data-staff="x2"]')).not.toBeNull();
        expect(magicHref.querySelector('.MagicLink-User[data-staff="x3"]')).not.toBeNull();

        expect(magicHref.querySelector('.MagicLink-User[data-staff="x1"].MagicLink-User_dismissed')).not.toBeNull();
        expect(getFirstLetterText('x1')).toStrictEqual('1');
        expect(getFirstLetterText('x2')).toStrictEqual('1');
        expect(getFirstLetterText('x3')).toStrictEqual('x');
    });

    it('Should skip unknown nodes', () => {
        const ctxElem = document.createElement('div');
        const enhancer = new MagicEnhancer();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        enhancer.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [
                { type: 'foobar' } as unknown as IMagicNode,
            ],
        });

        const magicHref = ctxElem.querySelector('.MagicLink') as HTMLAnchorElement;

        expect(magicHref.classList.contains('MagicLink'));
        expect(magicHref.childNodes.length).toStrictEqual(0);
    });

    it('Should capture href', () => {
        const enhancer = new MagicEnhancer();
        const href = createHref('https://href.com/1');

        enhancer.capture(href);

        expect(href.classList.contains('MagicLink-Capture')).toBe(true);
    });

    it('Should release href', () => {
        const enhancer = new MagicEnhancer();
        const href = createHref('https://href.com/1');

        href.classList.add('MagicLink-Capture');
        expect(href.classList.contains('MagicLink-Capture')).toBe(true);

        enhancer.release(href);

        expect(href.classList.contains('MagicLink-Capture')).toBe(false);
    });
});
