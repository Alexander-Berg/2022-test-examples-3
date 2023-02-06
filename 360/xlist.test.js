'use strict';

const xlist = require('./xlist.js');

const { CUSTOM_ERROR, HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });
const status = require('../_helpers/status');

let core;
let mockFolders;
let mockLabels;
let mockTabs;
let mockRevision;

const errorResponse = {
    error: {
        code: 0,
        message: 'Unknown error',
        reason: 'can\'t retrieve labels and folders: make_connection:can\'t get db connection from pool for mail_dsn(mdb304)' // eslint-disable-line max-len
    }
};

beforeEach(() => {
    mockFolders = jest.fn();
    mockLabels = jest.fn();
    mockTabs = jest.fn();
    mockRevision = jest.fn();

    core = {
        params: {},
        service: () => (method) => {
            if (method === '/folders') {
                return mockFolders();
            }
            if (method === '/labels') {
                return mockLabels();
            }
            if (method === '/v2/tabs') {
                return mockTabs();
            }
            return mockRevision();
        },
        req: {
            headers: {}
        }
    };
    core.status = status(core);
});

describe('-> TMP_FAIL в случае ошибки ручки', () => {
    beforeEach(() => {
        mockRevision.mockResolvedValueOnce({});
    });

    it('folders', async () => {
        mockFolders.mockRejectedValueOnce(new CUSTOM_ERROR(errorResponse));
        mockLabels.mockResolvedValueOnce({});

        const result = await xlist(core);

        expect(result[0].status.status).toBe(2);
    });

    it('labels', async () => {
        mockFolders.mockResolvedValueOnce({});
        mockLabels.mockRejectedValueOnce(new CUSTOM_ERROR(errorResponse));

        const result = await xlist(core);

        expect(result[0].status.status).toBe(2);
    });

    it('folders 500', async () => {
        mockFolders.mockRejectedValueOnce(httpError(500));
        mockLabels.mockResolvedValueOnce({});

        const result = await xlist(core);

        expect(result[0].status.status).toBe(2);
    });

    it('labels 500', async () => {
        mockFolders.mockResolvedValueOnce({});
        mockLabels.mockRejectedValueOnce(httpError(500));

        const result = await xlist(core);

        expect(result[0].status.status).toBe(2);
    });

    it('meta timeout', async () => {
        mockFolders.mockRejectedValueOnce(httpError(0));
        mockLabels.mockRejectedValueOnce(httpError(0));

        const result = await xlist(core);

        expect(result[0].status.status).toBe(2);
    });
});

test('-> OK happy path с эквивалентным md5 в запросе', async () => {
    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });
    core.params.md5 = '968bdb41512fa418916654f4b34e343d';

    const result = await xlist(core);

    expect(result).toHaveLength(1);
    expect(result[0].status.status).toBe(1);
    expect(result[0].md5).toBe('968bdb41512fa418916654f4b34e343d');
});

test('-> OK happy path', async () => {
    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result).toMatchSnapshot();
});

test('-> OK unsubscribe type == 12 (андроид >= 3.27)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/3.27.00000 (asus ASUS_T00J; Android 4.4.2)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[10]).toEqual({
        fid: '17',
        parent: '',
        display_name: 'Unsubscribe',
        count_unread: 0,
        count_all: 0,
        type: 12,
        unvisited: false,
        options: { position: '0' }
    });
});

test('-> OK unsubscribe type == 11 (андроид <= 3.26)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/3.26.46428 (samsung SM-G950F; Android 7.0)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[10]).toEqual({
        fid: '17',
        parent: '',
        display_name: 'Unsubscribe',
        count_unread: 0,
        count_all: 0,
        type: 11,
        unvisited: false,
        options: { position: '0' }
    });
});

test('-> OK unsubscribe type == 12 (иос)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/357.438 (iPhone8,1; iOS 11.2.6)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[10]).toEqual({
        fid: '17',
        parent: '',
        display_name: 'Unsubscribe',
        count_unread: 0,
        count_all: 0,
        type: 12,
        unvisited: false,
        options: { position: '0' }
    });
});

test('-> OK reply_later type == 15 (андроид >= 8.12.0)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/8.12.0 (asus ASUS_T00J; Android 4.4.2)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[12]).toEqual({
        fid: '96',
        parent: '',
        display_name: 'Reply Later',
        count_unread: 1,
        count_all: 13,
        type: 15,
        unvisited: false,
        options: { position: '4500' }
    });
});

test('-> OK reply_later type == 15 (андроид <= 8.11.1)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/8.10.46428 (samsung SM-G950F; Android 7.0)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[12]).toEqual({
        lid: '1',
        type: 3,
        color: '0',
        count_all: 0,
        count_unread: 0,
        display_name: '12'
    });
});

test('-> OK reply_later type == 15 (иос)', async () => {
    core.req.headers = {
        'user-agent': 'ru.yandex.mail/357.438 (iPhone8,1; iOS 11.2.6)'
    };

    mockFolders.mockResolvedValueOnce(require('../../../test/mock/folders.json'));
    mockLabels.mockResolvedValueOnce(require('../../../test/mock/labels.json'));
    mockRevision.mockResolvedValueOnce({ mailbox_revision: 123 });

    const result = await xlist(core);

    expect(result[12]).toEqual({
        fid: '96',
        parent: '',
        display_name: 'Reply Later',
        count_unread: 1,
        count_all: 13,
        type: 15,
        unvisited: false,
        options: { position: '4500' }
    });
});
