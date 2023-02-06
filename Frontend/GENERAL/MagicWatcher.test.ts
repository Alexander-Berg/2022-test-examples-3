import { delay } from './utils/time';
import { instanceId } from './utils/const';
import { MagicWatcher } from './MagicWatcher';

function createHref(url: string): HTMLAnchorElement {
    const href = document.createElement('a');

    href.setAttribute('href', url);
    href.appendChild(document.createTextNode('LINK!'));

    return href;
}

function createWrap(node: Node): Node {
    const wrapper = document.createElement('div');

    wrapper.appendChild(node);

    return wrapper;
}

describe('MagicWatcher', () => {
    it('Should be an instance of MagicWatcher', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        expect(watcher).toBeInstanceOf(MagicWatcher);
    });

    it('Should not handle changes if not attached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.runListener = jest.fn();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        await delay(100);

        ctxElem.removeChild(href);

        await delay(100);

        expect(watcher.runListener).not.toHaveBeenCalled();
    });

    it('Should not handle changes if detached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();
        watcher.detach();

        watcher.runListener = jest.fn();

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        await delay(100);

        ctxElem.removeChild(href);

        await delay(100);

        expect(watcher.runListener).not.toHaveBeenCalled();
    });

    it('Should handle basic href attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url, ctxElem, false);
    });

    it('Should handle basic href detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(href);

        await delay(100);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should not handle no href elem attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const elem = document.createElement('div');

        ctxElem.appendChild(elem);

        await delay(100);

        expect(handleAttachHref).not.toHaveBeenCalled();
    });

    it('Should not handle no href elem href detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const elem = document.createElement('div');

        ctxElem.appendChild(elem);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(elem);

        await delay(100);

        expect(handleDetachHref).not.toHaveBeenCalled();
    });

    it('Should not handle text attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const text = document.createTextNode('text');

        ctxElem.appendChild(text);

        await delay(100);

        expect(handleAttachHref).not.toHaveBeenCalled();
    });

    it('Should not handle text detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const text = document.createTextNode('text');

        ctxElem.appendChild(text);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(text);

        await delay(100);

        expect(handleDetachHref).not.toHaveBeenCalled();
    });

    it('Should handle wrapped href attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const url = 'https://href.com/1';
        const href = createHref(url);
        const wrap = createWrap(href);

        ctxElem.appendChild(wrap);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url, ctxElem, false);
    });

    it('Should handle wrapped href detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const wrap = createWrap(href);

        ctxElem.appendChild(wrap);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(wrap);

        await delay(100);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should handle nested href attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const url2 = 'https://href.com/2';
        const url1 = 'https://href.com/1';
        const href2 = createHref(url2);
        const href1 = createHref(url1);

        href1.appendChild(href2);

        ctxElem.appendChild(href1);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should handle nested href detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url2 = 'https://href.com/2';
        const url1 = 'https://href.com/1';
        const href2 = createHref(url2);
        const href1 = createHref(url1);

        href1.appendChild(href2);

        ctxElem.appendChild(href1);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(href1);

        await delay(100);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href1, url1, ctxElem);
    });

    it('Should handle wrapped nested href attachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        const url2 = 'https://href.com/2';
        const url1 = 'https://href.com/1';
        const href2 = createHref(url2);
        const href1 = createHref(url1);

        href1.appendChild(href2);

        const wrap = createWrap(href1);

        ctxElem.appendChild(wrap);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should handle wrapped nested href detachments', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url2 = 'https://href.com/2';
        const url1 = 'https://href.com/1';
        const href2 = createHref(url2);
        const href1 = createHref(url1);

        href1.appendChild(href2);

        const wrap = createWrap(href1);

        ctxElem.appendChild(wrap);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        ctxElem.removeChild(wrap);

        await delay(100);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href1, url1, ctxElem);
    });

    it('Should handle direct href changes', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href = createHref(url1);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();
        const handleAttachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);
        watcher.addListener('attachHref', handleAttachHref);

        href.setAttribute('href', url2);

        await delay(100);

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href, url1, ctxElem);
        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url2, ctxElem, false);
    });

    it('Should handle direct href set', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = document.createElement('a');

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleDetachHref = jest.fn();
        const handleAttachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);
        watcher.addListener('attachHref', handleAttachHref);

        href.setAttribute('href', url);

        await delay(100);

        expect(handleDetachHref).not.toHaveBeenCalled();
        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url, ctxElem, false);
    });

    it('Should update outer href if nested href attached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const nest = createHref('https://href.com/2');

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.appendChild(nest);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested href detached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const nest = createHref('https://href.com/2');

        href.appendChild(nest);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.removeChild(nest);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested elem attached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const elem = document.createElement('span');

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.appendChild(elem);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested elem detached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const elem = document.createElement('span');

        href.appendChild(elem);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.removeChild(elem);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested text attached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const text = document.createTextNode('text');

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.appendChild(text);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested text detached', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const text = document.createTextNode('text');

        href.appendChild(text);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.removeChild(text);

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should handle direct attr changes', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        href.setAttribute('foo', 'bar');

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should handle nested attr changes', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const nest = document.createElement('span');

        href.appendChild(nest);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        nest.setAttribute('foo', 'bar');

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should not handle no href attr changes', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const elem = document.createElement('div');

        ctxElem.appendChild(elem);
        document.body.appendChild(ctxElem);

        watcher.attach();

        watcher.runListener = jest.fn();

        elem.setAttribute('href', 'foo');

        await delay(100);

        expect(watcher.runListener).not.toHaveBeenCalled();
    });

    it('Should update outer href if direct text changed', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const text = document.createTextNode('text');

        href.appendChild(text);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        text.data += 'test';

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should update outer href if nested text changed', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);
        const text = document.createTextNode('text');
        const wrap = createWrap(text);

        href.appendChild(wrap);

        ctxElem.appendChild(href);
        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleUpdateHref = jest.fn();

        watcher.addListener('updateHref', handleUpdateHref);

        text.data += 'test';

        await delay(100);

        expect(handleUpdateHref).toHaveBeenCalledTimes(1);
        expect(handleUpdateHref).toHaveBeenCalledWith(href, url, ctxElem);
    });

    it('Should not handle href less text changes', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const text = document.createTextNode('text');

        ctxElem.appendChild(text);
        document.body.appendChild(ctxElem);

        watcher.attach();

        watcher.runListener = jest.fn();

        text.data += 'test';

        await delay(100);

        expect(watcher.runListener).not.toHaveBeenCalled();
    });

    it('Should run attachHref on found hrefs on simulateAttach', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        watcher.simulateAttach();

        expect(handleAttachHref).toHaveBeenCalledTimes(2);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
        expect(handleAttachHref).toHaveBeenCalledWith(href2, url2, ctxElem, false);
    });

    it('Should run attachHref on found hrefs on simulateAttach (nested href)', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        href1.appendChild(href2);

        ctxElem.appendChild(href1);

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        watcher.simulateAttach();

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should run detachHref on found hrefs on simulateDetach', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        watcher.simulateDetach();

        expect(handleDetachHref).toHaveBeenCalledTimes(2);
        expect(handleDetachHref).toHaveBeenCalledWith(href1, url1, ctxElem);
        expect(handleDetachHref).toHaveBeenCalledWith(href2, url2, ctxElem);
    });

    it('Should run detachHref on found hrefs on simulateDetach (nested href)', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        href1.appendChild(href2);

        ctxElem.appendChild(href1);

        const handleDetachHref = jest.fn();

        watcher.addListener('detachHref', handleDetachHref);

        watcher.simulateDetach();

        expect(handleDetachHref).toHaveBeenCalledTimes(1);
        expect(handleDetachHref).toHaveBeenCalledWith(href1, url1, ctxElem);
    });

    it('Should ignore special marked hrefs', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({
            ctxElem,
        });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        href2.setAttribute('data-magic-id', instanceId);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        watcher.simulateAttach();

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should ignore relative hrefs', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({
            ctxElem,
        });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = '/rel';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        watcher.simulateAttach();

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should support href filtering', () => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({
            ctxElem,
            filter(href, url) {
                return !url.includes('2');
            },
        });

        document.body.appendChild(ctxElem);

        const url1 = 'https://href.com/1';
        const url2 = 'https://href.com/2';
        const href1 = createHref(url1);
        const href2 = createHref(url2);

        ctxElem.appendChild(href1);
        ctxElem.appendChild(href2);

        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        watcher.simulateAttach();

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url1, ctxElem, false);
    });

    it('Should support in handler attach/detach', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href = createHref(url);

        document.body.appendChild(ctxElem);

        watcher.attach();

        const handleAttachHref = jest.fn(() => {
            watcher.detach();
            ctxElem.appendChild(href.cloneNode());
            watcher.attach();
        });

        watcher.addListener('attachHref', handleAttachHref);

        ctxElem.appendChild(href);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(1);
        expect(handleAttachHref).toHaveBeenCalledWith(href, url, ctxElem, false);
    });

    it('Should support immutable operations', async() => {
        const ctxElem = document.createElement('div');
        const watcher = new MagicWatcher({ ctxElem });

        const url = 'https://href.com/1';
        const href1 = createHref(url);
        const href2 = createHref(url);
        const href3 = createHref(url);
        const href4 = createHref(url);
        const href5 = createHref(url);

        document.body.appendChild(ctxElem);

        watcher.attach();
        const handleAttachHref = jest.fn();

        watcher.addListener('attachHref', handleAttachHref);

        ctxElem.appendChild(href1);

        await delay(100);

        watcher.mutate(() => {
            ctxElem.appendChild(href2);

            watcher.mutate(() => {
                ctxElem.appendChild(href3);
            });

            ctxElem.appendChild(href4);
        });

        ctxElem.appendChild(href5);

        await delay(100);

        expect(handleAttachHref).toHaveBeenCalledTimes(2);
        expect(handleAttachHref).toHaveBeenCalledWith(href1, url, ctxElem, false);
        expect(handleAttachHref).toHaveBeenCalledWith(href5, url, ctxElem, false);
    });
});
