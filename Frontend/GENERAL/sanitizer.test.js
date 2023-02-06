const { describe, it } = require('mocha');
const { expect } = require('chai');

const { formatMdAst } = require('../index');

describe('sanitizer', () => {
    it('should sanitize ((url desc))', async() => {
        const res = await formatMdAst({ type: 'root', children: [{ type: 'paragraph', children: [{ type: 'womLink', url: 'javascript://any_xss', brackets: false, children: [{ type: 'text', value: 'xss' }] }] }] });

        expect(res).to.deep.equal({
            block: 'wiki-doc',
            content: {
                block: 'wiki-p',
                content: {
                    block: 'wiki-ref',
                    content: {
                        block: 'wiki-txt',
                        'wiki-attrs': {
                            txt: 'xss',
                        },
                    },
                    url: '#sanitized',
                },
            },
            mods: {},
            toc: [],
            'wiki-attrs': {},
        });
    });

    it('should sanitize [xss](javascript://any_xss)', async() => {
        const res = await formatMdAst({ type: 'root', children: [{ type: 'paragraph', children: [{ type: 'link', title: null, url: 'javascript://any_xss', children: [{ type: 'text', value: 'xss' }] }] }] });

        expect(res).to.deep.equal({
            block: 'wiki-doc',
            content: {
                block: 'wiki-p',
                content: {
                    block: 'wiki-ref',
                    content: {
                        block: 'wiki-txt',
                        'wiki-attrs': {
                            txt: 'xss',
                        },
                    },
                    ref: undefined,
                    title: null,
                    url: '#sanitized',
                },
            },
            mods: {},
            toc: [],
            'wiki-attrs': {},
        });
    });

    it('should not sanitize [valid](http://any_xss)', async() => {
        const res = await formatMdAst({ type: 'root', children: [{ type: 'paragraph', children: [{ type: 'link', title: null, url: 'http://any_xss', children: [{ type: 'text', value: 'valid' }] }] }] });

        expect(res).to.deep.equal({
            block: 'wiki-doc',
            content: {
                block: 'wiki-p',
                content: {
                    block: 'wiki-ref',
                    content: {
                        block: 'wiki-txt',
                        'wiki-attrs': {
                            txt: 'valid',
                        },
                    },
                    ref: undefined,
                    title: null,
                    url: 'http://any_xss',
                },
            },
            mods: {},
            toc: [],
            'wiki-attrs': {},
        });
    });

    it('should not sanitize ((http://url desc))', async() => {
        const res = await formatMdAst({ type: 'root', children: [{ type: 'paragraph', children: [{ type: 'womLink', url: 'http://any_xss', brackets: false, children: [{ type: 'text', value: 'valid' }] }] }] });

        expect(res).to.deep.equal({
            block: 'wiki-doc',
            content: {
                block: 'wiki-p',
                content: {
                    block: 'wiki-ref',
                    content: {
                        block: 'wiki-txt',
                        'wiki-attrs': {
                            txt: 'valid',
                        },
                    },
                    url: 'http://any_xss',
                },
            },
            mods: {},
            toc: [],
            'wiki-attrs': {},
        });
    });
});
