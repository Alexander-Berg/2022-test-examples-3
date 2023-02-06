import { delay } from './utils/time';
import { MagicSupplier } from './MagicSupplier';
import { MagicEnhancer } from './MagicEnhancer';

function createHref(url: string): HTMLAnchorElement {
    const href = document.createElement('a');

    href.setAttribute('href', url);
    href.appendChild(document.createTextNode('LINK'));

    return href;
}

describe('MagicSupplier', () => {
    it('Should be an instance of MagicSupplier', () => {
        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        });

        expect(supplier).toBeInstanceOf(MagicSupplier);
    });

    it('Should simulate attachHref on addContext', async() => {
        const ctxElem = document.createElement('div');
        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        const handleAttachHref = jest.fn();
        const handleDetachHref = jest.fn();

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addListener('attachHref', handleAttachHref)
            .addListener('detachHref', handleDetachHref);

        supplier.addContext(ctxElem);

        await delay(500);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url, ctxElem);
        expect(handleDetachHref).toHaveBeenCalledTimes(0);
    });

    it('Should simulate detachHref on delContext', async() => {
        const ctxElem = document.createElement('div');
        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        const handleDetachHref = jest.fn();

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addListener('detachHref', handleDetachHref);

        supplier.addContext(ctxElem);

        await delay(100);

        supplier.delContext(ctxElem);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should run updateHref on update href', async() => {
        const ctxElem = document.createElement('div');
        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        const handleUpdateHref = jest.fn();

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addListener('updateHref', handleUpdateHref);

        supplier.addContext(ctxElem);

        await delay(100);

        href.appendChild(document.createTextNode('foo'));

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should not fail on double equal addContext/delContext', async() => {
        const ctxElem = document.createElement('div');
        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        const handleAttachHref = jest.fn();
        const handleDetachHref = jest.fn();

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addListener('attachHref', handleAttachHref)
            .addListener('detachHref', handleDetachHref);

        supplier.addContext(ctxElem);

        await delay(500);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);

        supplier.addContext(ctxElem);

        await delay(500);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);

        supplier.delContext(ctxElem);

        await delay(500);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);

        supplier.delContext(ctxElem);

        await delay(500);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
    });

    it('Should support filtering for context', async() => {
        const handleAttachHref = jest.fn();
        const handleDetachHref = jest.fn();
        const ctxElem = document.createElement('div');

        document.body.appendChild(ctxElem);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        });

        supplier
            .addListener('attachHref', handleAttachHref)
            .addListener('detachHref', handleDetachHref)
            .addContext(ctxElem, {
                filter: (href, url) => !url.includes('2'),
            });

        await delay(100);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';

        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        await delay(500);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem);

        ctxElem.removeChild(href1);
        ctxElem.removeChild(href2);

        await delay(500);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href1, url1, ctxElem);
    });

    it('Should manage link data usage', async() => {
        const ctxElem1 = document.createElement('div');
        const ctxElem2 = document.createElement('div');
        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem1.appendChild(href1);
        ctxElem2.appendChild(href2);
        document.body.appendChild(ctxElem1);
        document.body.appendChild(ctxElem2);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        });

        supplier.addContext(ctxElem1);

        await delay(100);

        expect(supplier.hasUsage(url1)).toBe(true);
        expect(supplier.hasUsage(url2)).toBe(false);

        supplier.setUsage(url1, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [],
        });

        supplier.setUsage(url2, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [],
        });

        expect(href1.classList.contains('MagicLink-Hidden')).toBe(true);
        expect(href2.classList.contains('MagicLink-Hidden')).toBe(false);

        supplier.delUsage(url1);
        supplier.delUsage(url2);

        expect(href1.classList.contains('MagicLink-Hidden')).toBe(false);
        expect(href2.classList.contains('MagicLink-Hidden')).toBe(false);
    });

    it('Should enhance/reverse/capture/release tools', () => {
        const url = 'https://href.com/1';
        const href = createHref(url);
        const ctxElem = document.createElement('div');

        document.body.appendChild(ctxElem);

        ctxElem.appendChild(href);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addContext(ctxElem);

        supplier.capture(href);

        expect(href.classList.contains('MagicLink-Capture')).toBe(true);

        supplier.release(href);

        expect(href.classList.contains('MagicLink-Capture')).toBe(false);

        supplier.enhance(href, url, {
            completed: true,
            ttl: 0,
            now: 0,
            type: 'list',
            separator: '',
            value: [],
        });

        expect(href.classList.contains('MagicLink-Hidden')).toBe(true);

        supplier.reverse(href);

        expect(href.classList.contains('MagicLink-Hidden')).toBe(false);
    });

    it('Should not fail on unknown hrefs', () => {
        const url = 'https://href.com/1';
        const href = createHref(url);
        const ctxElem = document.createElement('div');

        document.body.appendChild(ctxElem);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addContext(ctxElem);

        expect(() => {
            supplier.capture(href);
            supplier.release(href);
            supplier.enhance(href, url, {
                completed: true,
                ttl: 0,
                now: 0,
                type: 'list',
                separator: '',
                value: [],
            });
            supplier.reverse(href);
        }).not.toThrow();
    });

    it('Should return true if any context added', () => {
        const ctxElem = document.createElement('div');

        document.body.appendChild(ctxElem);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        })
            .addContext(ctxElem);

        expect(supplier.anyContext()).toBe(true);

        supplier.delContext(ctxElem);

        expect(supplier.anyContext()).toBe(false);
    });

    it('Should graceful handle nested contexts', async() => {
        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const url3 = 'https://href.com/3';
        const href1 = createHref(url1);
        const href2 = createHref(url2);
        const href3 = createHref(url3);
        const ctxElem1 = document.createElement('div');
        const ctxElem2 = document.createElement('div');
        const ctxElem3 = document.createElement('div');

        ctxElem1.appendChild(href1);
        ctxElem2.appendChild(href2);
        ctxElem3.appendChild(href3);

        document.body.appendChild(ctxElem1);
        document.body.appendChild(ctxElem3);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        });

        const handleAttachHref = jest.fn((href, url) => {
            supplier.enhance(href, url, {
                completed: true,
                ttl: 0,
                now: 0,
                type: 'list',
                separator: '',
                value: [],
            });
        });
        const handleDetachHref = jest.fn();
        const handleUpdateHref = jest.fn();

        supplier.addListener('attachHref', handleAttachHref);
        supplier.addListener('detachHref', handleDetachHref);
        supplier.addListener('updateHref', handleUpdateHref);

        supplier.addContext(ctxElem1);
        ctxElem1.appendChild(ctxElem2);
        supplier.addContext(ctxElem2);
        supplier.addContext(ctxElem3);

        await delay(200);

        expect(handleAttachHref).toHaveBeenCalledTimes(3);
        expect(handleUpdateHref).toHaveBeenCalledTimes(0);
        expect(handleDetachHref).toHaveBeenCalledTimes(0);
    });

    it('Should graceful handle nested contexts even if disconnected context added', async() => {
        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const url3 = 'https://href.com/3';
        const href1 = createHref(url1);
        const href2 = createHref(url2);
        const href3 = createHref(url3);
        const ctxElem1 = document.createElement('div');
        const ctxElem2 = document.createElement('div');
        const ctxElem3 = document.createElement('div');

        ctxElem1.appendChild(href1);
        ctxElem2.appendChild(href2);
        ctxElem3.appendChild(href3);

        document.body.appendChild(ctxElem1);
        document.body.appendChild(ctxElem3);

        const supplier = new MagicSupplier({
            enhancer: new MagicEnhancer(),
        });

        const handleAttachHref = jest.fn((href, url) => {
            supplier.enhance(href, url, {
                completed: true,
                ttl: 0,
                now: 0,
                type: 'list',
                separator: '',
                value: [],
            });
        });
        const handleDetachHref = jest.fn();
        const handleUpdateHref = jest.fn();

        supplier.addListener('attachHref', handleAttachHref);
        supplier.addListener('detachHref', handleDetachHref);
        supplier.addListener('updateHref', handleUpdateHref);

        supplier.addContext(ctxElem1);
        supplier.addContext(ctxElem2);
        supplier.addContext(ctxElem3);

        ctxElem1.appendChild(ctxElem2);

        await delay(200);

        expect(handleAttachHref).toHaveBeenCalledTimes(3);
        expect(handleUpdateHref).toHaveBeenCalledTimes(0);
        expect(handleDetachHref).toHaveBeenCalledTimes(0);

        handleAttachHref.mockReset();
        handleUpdateHref.mockReset();
        handleDetachHref.mockReset();

        ctxElem2.removeChild(href2);

        await delay(200);

        expect(handleAttachHref).toHaveBeenCalledTimes(0);
        expect(handleUpdateHref).toHaveBeenCalledTimes(0);
        expect(handleDetachHref).toHaveBeenCalledTimes(1);

        handleAttachHref.mockReset();
        handleUpdateHref.mockReset();
        handleDetachHref.mockReset();

        ctxElem2.appendChild(href2);

        await delay(200);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledTimes(0);
        expect(handleDetachHref).toHaveBeenCalledTimes(0);
    });
});
