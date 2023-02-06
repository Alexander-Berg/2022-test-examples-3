'use strict';

describe('routes/send/convert-params', () => {
    const convertParams = require('./convert-params.js');

    it('Skips empty values', () => {
        const params = {
            from_mailbox: '',
            from_name: 2
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual({
            sender: {
                from_name: 2
            }
        });
    });

    it('Converts simple params', () => {
        const params = {
            from_mailbox: 1,
            from_name: 2,
            to: 'to',
            bcc: 'bcc',
            cc: 'cc',
            subj: 'subj',
            send: 'text',
            ttype: 'plain',
            overwrite: 'mid',
            captcha_entered: '123',
            captcha_key: 'key',
            lids: [ 'lid1', 'lid2' ],
            references: 'ref'
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual({
            sender: {
                from_mailbox: 1,
                from_name: 2
            },
            message: {
                subj: 'subj',
                text: 'text',
                html: false,
                source_mid: 'mid'
            },
            recipients: {
                to: 'to',
                cc: 'cc',
                bcc: 'bcc'
            },
            references: 'ref',
            lids: [ 'lid1', 'lid2' ],
            captcha: {
                captcha_key: 'key',
                captcha_entered: '123'
            }
        });
    });

    it('Converts lids', () => {
        const params = {
            lids: 'one-lid'
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual({
            lids: [ 'one-lid' ]
        });
    });

    // In reply to
    it.each([
        [ 'repl', 'replied', { inreplyto: 'repl', mark_as: 'replied' } ],
        [ 'repl', 'forwarded', { inreplyto: 'repl', mark_as: 'forwarded' } ],
        [ 'repl', 'wrong', { inreplyto: 'repl' } ],
        [ 'repl', null, { inreplyto: 'repl' } ],
        [ null, 'replied', null ]
    ])('inreplyto=%s, mark_as=%s', (inreplyto, mark_as, expected) => { // eslint-disable-line camelcase
        const params = {
            inreplyto,
            mark_as
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual(expected ? { inreplyto: expected } : {});
    });

    it('Converts inreplyto for save-draft', () => {
        const params = {
            inreplyto: 'repl',
            mark_as: 'forwarded'
        };
        const result = convertParams(params, 'save-draft');
        expect(result).toEqual({
            inreplyto: 'repl'
        });
    });

    it('Converts simple attaches', () => {
        const params = {
            att_ids: [ 1, 2, 3 ],
            ids: [ 1, 2 ],
            parts: [ 1, 2, 3 ]
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual({
            attaches: {
                uploaded_attach_stids: [ 1, 2, 3 ],
                forward_mids: [ 1, 2 ]
            }
        });
    });

    it('Converts parts', () => {
        const params = {
            parts: [ 7, 8 ],
            overwrite: 123,
            fwd_ids: 555
        };
        const result = convertParams(params, 'send-message');
        const parts = result.attaches.parts_json.map((json) => JSON.parse(json));
        expect(parts).toEqual([
            { hid: 7, mid: 123, rotate: '0' },
            { hid: 8, mid: 123, rotate: '0' }
        ]);
    });

    it('Converts parts for forwarded email', () => {
        const params = {
            parts: [ 7, 8 ],
            fwd_ids: 555
        };
        const result = convertParams(params, 'send-message');
        const parts = result.attaches.parts_json.map((json) => JSON.parse(json));
        expect(parts).toEqual([
            { hid: 7, mid: 555, rotate: '0' },
            { hid: 8, mid: 555, rotate: '0' }
        ]);
    });

    it('Skips parts for several forwarded emails', () => {
        const params = {
            parts: [ 7, 8 ],
            fwd_ids: [ 123, 555 ]
        };
        const result = convertParams(params, 'send-message');
        expect(result).toEqual({});
    });
});
