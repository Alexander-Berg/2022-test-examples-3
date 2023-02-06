let attachScriptNode;

const scriptSrc = 'https://yastatic.net/metrika.js';

describe('attachScriptNode', () => {
    beforeEach(() => {
        jest.resetModules();

        attachScriptNode = require('../attach-script-node').attachScriptNode;
    });

    afterEach(() => {
        document.getElementsByTagName('html')[0].innerHTML = '';
    });

    test('should append script to document', () => {
        expect(document.querySelector(`script[src="${scriptSrc}"]`)).toBeNull();

        attachScriptNode(scriptSrc);

        const script = document.querySelector(`script[src="${scriptSrc}"]`);

        expect(script).toBeInstanceOf(HTMLScriptElement);
    });

    test('should set default attributes', () => {
        attachScriptNode(scriptSrc);

        const script: HTMLScriptElement = document.querySelector(`script[src="${scriptSrc}"]`);

        expect(script.async).toBe(true);
    });

    test('should set only attribute defer', () => {
        attachScriptNode(scriptSrc, { defer: true });

        const script: HTMLScriptElement = document.querySelector(`script[src="${scriptSrc}"]`);

        expect(script.defer).toBe(true);
        expect(script.async).toBeUndefined();
    });

    test('should resolve promise on load', async() => {
        const nodePromise = attachScriptNode(scriptSrc);
        const script: HTMLScriptElement = document.querySelector(`script[src="${scriptSrc}"]`);

        // @ts-ignore
        script.readyState = 'loaded';
        script.onload({} as Event);

        await expect(nodePromise).resolves.toEqual(script);
    });

    test('should reject promise on error', async() => {
        const nodePromise = attachScriptNode(scriptSrc);
        const script: HTMLScriptElement = document.querySelector(`script[src="${scriptSrc}"]`);
        const scriptError = `Can't load script with url - ${scriptSrc}`;

        script.onerror('');

        await expect(nodePromise).rejects.toMatch(scriptError);
    });
});
