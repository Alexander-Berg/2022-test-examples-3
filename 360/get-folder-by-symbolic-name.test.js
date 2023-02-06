'use strict';

const getFolderBySymbolicName = require('./get-folder-by-symbolic-name.js');

const MUST_REJECT = () => Promise.reject('MUST_REJECT');

let core;
let meta;

beforeEach(() => {
    meta = jest.fn();
    core = {
        services: {
            meta
        },
        service: jest.fn().mockReturnValue(meta)
    };
});

test('finds folder', async () => {
    meta.mockResolvedValueOnce(require('../../../test/mock/folders.json'));

    const res = await getFolderBySymbolicName(core, 'inbox');

    expect(res).toMatchSnapshot();
});

test('non existing folder', async () => {
    meta.mockResolvedValueOnce(require('../../../test/mock/folders.json'));

    const res = await getFolderBySymbolicName(core, 'abrakadabra');

    expect(res).toBeNull();
});

test('rejects with error', async () => {
    meta.mockRejectedValueOnce({ error: 'error' });

    try {
        await getFolderBySymbolicName(core, 'inbox');
        MUST_REJECT();
    } catch (err) {
        expect(err).toEqual({ error: 'error' });
    }
});
