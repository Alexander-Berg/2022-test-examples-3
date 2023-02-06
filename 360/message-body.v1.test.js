'use strict';

const s = require('serializr');
const messageBodySchema = require('./message-body.v1.js');
const deserialize = s.deserialize.bind(s, messageBodySchema);

describe('info', () => {
    it('returns meta info', () => {
        const result = deserialize({
            info: {
                addressesResult: [
                    {
                        direction: 'from',
                        name: 'Alpha',
                        email: 'one@example.com'
                    },
                    {
                        direction: 'to',
                        name: 'Beta',
                        email: 'two@example.com'
                    },
                    {
                        direction: 'to',
                        name: 'Gamma',
                        email: 'three@example.com'
                    },
                    {
                        direction: 'cc',
                        name: 'Delta',
                        email: 'four@example.com'
                    },
                    {
                        direction: 'bcc',
                        name: 'Epsilon',
                        email: 'five@example.com'
                    },
                    {
                        direction: 'reply-to',
                        name: 'Zeta',
                        email: 'six@example.com'
                    }
                ]
            }
        });

        expect(result).toEqual({
            from: {
                displayName: 'Alpha',
                email: 'one@example.com'
            },
            to: [
                {
                    displayName: 'Beta',
                    email: 'two@example.com'
                },
                {
                    displayName: 'Gamma',
                    email: 'three@example.com'
                }
            ],
            cc: [
                {
                    displayName: 'Delta',
                    email: 'four@example.com'
                }
            ],
            bcc: [
                {
                    displayName: 'Epsilon',
                    email: 'five@example.com'
                }
            ],
            replyTo: [
                {
                    displayName: 'Zeta',
                    email: 'six@example.com'
                }
            ]
        });
    });
});

describe('content', () => {
    it('returns content info', () => {
        const result = deserialize({
            bodies: [
                {
                    transformerResult: {
                        textTransformerResult: {
                            content: 'foo',
                            typeInfo: {
                                contentType: {
                                    type: 'text',
                                    subtype: 'plain'
                                }
                            },
                            isRaw: true,
                            isTrimmed: true
                        }
                    }
                }
            ]
        });

        expect(result).toEqual({
            content: 'foo',
            contentType: 'text/plain',
            isRaw: true,
            isTrimmed: true
        });
    });

    it('handles non-first body', () => {
        const result = deserialize({
            bodies: [
                {},
                {
                    transformerResult: {
                        textTransformerResult: {
                            content: 'foo',
                            typeInfo: {
                                contentType: {
                                    type: 'text',
                                    subtype: 'plain'
                                }
                            },
                            isRaw: true,
                            isTrimmed: true
                        }
                    }
                }
            ]
        });

        expect(result).toEqual({
            content: 'foo',
            contentType: 'text/plain',
            isRaw: true,
            isTrimmed: true
        });
    });

    it('handles no body', () => {
        const result = deserialize({
            bodies: []
        });

        expect(result).toEqual({
            content: '',
            isRaw: false,
            isTrimmed: false
        });
    });
});

describe('attachments', () => {
    it('returns attachments info', () => {
        const result = deserialize({
            attachments: [
                {
                    binaryTransformerResult: {
                        hid: '123',
                        cid: 'foo',
                        typeInfo: {
                            contentType: {
                                type: 'text',
                                subtype: 'plain'
                            },
                            name: 'bar'
                        },
                        isInline: true
                    }
                },
                {
                    messageTransformerResult: {
                        hid: '456',
                        cid: 'baz',
                        typeInfo: {
                            contentType: {
                                type: 'text',
                                subtype: 'plain'
                            },
                            name: 'qux'
                        },
                        isInline: false
                    }
                },
                {
                    narodTransformerResult: [
                        {
                            fakeHid: '789',
                            name: 'xyz',
                            url: 'http://example.com'
                        }
                    ]
                },
                {
                    unknownTransformerResult: {}
                }
            ]
        });

        expect(result).toEqual({
            attachments: [
                {
                    id: '123',
                    contentId: 'foo',
                    contentType: 'text/plain',
                    filename: 'bar',
                    inline: true
                },
                {
                    id: '456',
                    contentId: 'baz',
                    contentType: 'text/plain',
                    filename: 'qux',
                    inline: false
                }
            ],
            diskAttachments: [
                {
                    id: '789',
                    filename: 'xyz',
                    url: 'http://example.com'
                }
            ]
        });
    });
});
