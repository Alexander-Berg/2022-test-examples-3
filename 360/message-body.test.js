'use strict';

const s = require('@ps-int/mail-lib').helpers.serializr;
const messageBodySchema = require('./message-body.js');
const deserialize = s.deserialize.bind(s, messageBodySchema);

describe('messageBodySchema', () => {
    describe('bodies', () => {
        const wrap = (obj) => ({ transformerResult: { textTransformerResult: obj } });

        it('converts main body', () => {
            const result = deserialize({ bodies: [ wrap({
                isMain: true,
                hid: '1.1',
                isRaw: true,
                isTrimmed: true,
                content: 'letter',
                contentTransformersResult: {
                    isPhishing: true
                },
                typeInfo: {
                    contentType: {
                        type: 't',
                        subtype: 's'
                    }
                }
            }) ] });

            expect(result.bodies).toEqual([ {
                hid: '1.1',
                type: 't',
                subtype: 's',
                content: 'letter',
                isRaw: true,
                isTrimmed: true,
                main: '',
                phishing: ''
            } ]);
        });

        it('converts empty body', () => {
            const result = deserialize({ bodies: [ wrap({}) ] });
            expect(result.bodies).toEqual([ {
                content: '',
                isRaw: false,
                isTrimmed: false,
                main: undefined,
                phishing: undefined
            } ]);
        });

        it('skips invalid body', () => {
            const result = deserialize({ bodies: [ {} ] });
            expect(result.bodies).toEqual([]);
        });
    });

    describe('attachments', () => {
        it('converts regular attachments', () => {
            const result = deserialize({ attachments: [
                {
                    binaryTransformerResult: {
                        hid: 'hid',
                        typeInfo: {
                            contentType: {
                                type: 'image',
                                subtype: 'png'
                            },
                            name: 'pic',
                            nameUriEncoded: 'pic-uri',
                            length: 13,
                            partClassInfo: {
                                partClass_: 'image',
                                isPreviewSupported_: true,
                                doesBrowserSupport_: true
                            }
                        }
                    }
                }, {
                    messageTransformerResult: {
                        hid: 'hid2',
                        isInline: true,
                        cid: 'cid2',
                        typeInfo: {
                            contentType: {
                                type: 'application',
                                subtype: 'json'
                            },
                            name: 'JSON',
                            nameUriEncoded: 'JSON',
                            length: 42,
                            partClassInfo: {
                                partClass_: 'general',
                                isPreviewSupported_: false,
                                doesBrowserSupport_: false
                            }
                        },
                        externalTransformersSelector: {
                            transformer: 'openoffice',
                            filetype: 'txt'
                        }
                    }
                }, {
                    skip: 'me'
                }
            ] });

            expect(result.attachments).toEqual([
                {
                    'hid': 'hid',
                    'cid': undefined,
                    'inline': undefined,
                    'type': 'image',
                    'subtype': 'png',
                    'name': 'pic',
                    'name-uri-encoded': 'pic-uri',
                    'length': 13,
                    'class': 'image',
                    'browser-supports': ''
                }, {
                    'hid': 'hid2',
                    'cid': 'cid2',
                    'inline': '',
                    'type': 'application',
                    'subtype': 'json',
                    'name': 'JSON',
                    'name-uri-encoded': 'JSON',
                    'length': 42,
                    'class': 'general',
                    'external_transformer': 'openoffice',
                    'external_transformer_filetype': 'txt'
                }
            ]);
        });

        it('converts disk attachments', () => {
            const result = deserialize({ attachments: [ {
                narodTransformerResult: [
                    {
                        fakeHid: 'hid',
                        name: 'name',
                        url: 'url',
                        sizeDescription: '13',
                        partClassInfo: {
                            partClass_: 'image',
                            isPreviewSupported_: true,
                            doesBrowserSupport_: true
                        }
                    }, {
                        fakeHid: 'hid2',
                        name: 'name2',
                        url: 'url2',
                        sizeDescription: '42',
                        partClassInfo: {
                            partClass_: 'general',
                            isPreviewSupported_: false,
                            doesBrowserSupport_: false
                        }
                    }
                ]
            } ] });

            expect(result.attachments).toEqual([
                {
                    'narod': '',
                    'hid': 'hid',
                    'type': 'application',
                    'subtype': 'octet-stream',
                    'name': 'name',
                    'url': 'url',
                    'size': '13',
                    'class': 'image',
                    'browser-supports': ''
                }, {
                    narod: '',
                    hid: 'hid2',
                    type: 'application',
                    subtype: 'octet-stream',
                    name: 'name2',
                    url: 'url2',
                    size: '42',
                    class: 'general'
                }
            ]);
        });
    });

    describe('info', () => {
        it('converts simple info', () => {
            const result = deserialize({ info: {
                inReplyTo: 'a',
                messageId: 'b',
                references: 'c',
                spam: 'd'
            } });

            expect(result.info).toEqual({
                'in-reply-to': 'a',
                'message-id': 'b',
                'references': 'c',
                'spam': 'd'
            });
        });

        it('converts addresses', () => {
            const result = deserialize({ info: {
                addressesResult: [
                    { direction: 'to', name: '1', email: 'a' },
                    { direction: 'from', name: '2', email: 'b' },
                    { direction: 'from', name: '3', email: 'c' },
                    { direction: 'to', name: '4', email: 'd' },
                    { direction: 'cc', name: '5', email: 'e' },
                    { direction: 'to', name: '6', email: 'f' },
                    { direction: 'bcc', name: '7', email: 'g' },
                    { direction: 'cc', name: '8', email: 'h' },
                    { direction: 'reply-to', name: '9', email: 'e' }
                ]
            } });

            expect(result.info).toEqual({
                'from': { displayName: '2', email: 'b' },
                'to': [
                    { displayName: '1', email: 'a' },
                    { displayName: '4', email: 'd' },
                    { displayName: '6', email: 'f' }
                ],
                'cc': [
                    { displayName: '5', email: 'e' },
                    { displayName: '8', email: 'h' }
                ],
                'bcc': [
                    { displayName: '7', email: 'g' }
                ],
                'reply-to': [
                    { displayName: '9', email: 'e' }
                ]
            });
        });
    });
});
