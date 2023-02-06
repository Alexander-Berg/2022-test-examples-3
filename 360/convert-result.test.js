'use strict';

describe('routes/send/convert-result', () => {
    const convertResult = require('./convert-result.js');

    it('Converts simple params', () => {
        const result = convertResult({
            status: 'ok',
            object: {
                messageId: 1,
                stored: {
                    mid: 2,
                    fid: 3
                },
                attachments: [ {
                    clas: 6,
                    name_uri_encoded: 7,
                    field: 8
                } ]
            }
        });
        expect(result).toEqual({
            message_id: 1,
            storedmid: 2,
            store_fid: 3,
            attachment: [ {
                'class': 6,
                'name-uri-encoded': 7,
                'field': 8
            } ],
            limited: undefined,
            status: 'ok'
        });
    });

    it('captcha', () => {
        const result = convertResult({
            status: 'captcha',
            object: {
                captcha: {
                    key: 4,
                    url: 5
                }
            }
        });
        expect(result).toEqual({
            captcha_key: 4,
            captcha_url: 5,
            status: 'captcha_request'
        });
    });

    it('limited', () => {
        // for coverage
        convertResult({ object: { limited: [] } });
        const result = convertResult({
            status: 'ok',
            object: {
                limited: [ {
                    local: 'l',
                    domain: 'd'
                } ]
            }
        });
        expect(result).toEqual({
            limited: {
                recipient: [ {
                    local: 'l',
                    domain: 'd',
                    $t: 'd'
                } ]
            },
            status: 'msg_too_big'
        });
    });

    it.each([
        [ 'bad_request', 'badbad', 'badbad' ],
        [ 'bad_request', null, 'illegal_params' ],
        [ 'spam', 'spamer', 'spamer' ],
        [ 'limited', null, 'msg_too_big' ],
        [ 'message_saved', null, 'internal_error' ],
        [ 'retryable_error', null, 'internal_error' ],
        [ 'wrong_ticket', null, 'internal_error' ],
        [ 'cannot_save', null, 'internal_error' ],
        [ 'wtf', null, 'wtf' ]
    ])('status=%s, reason=%s', (status, reason, expected) => {
        const result = convertResult({ status, reason });
        expect(result).toEqual({
            status: expected
        });
    });
});
