'use strict';

const method = require('./messages_by_timestamp_range');

const filteredLabelsMock = require('../../../test/mock/filtered-labels.json');
const messagesByFolderMock = require('../../../test/mock/meta/messages-by-folder.json');
const taksaMock = require('../../../test/mock/taksa.json');

const messagesSchema = require('../_helpers/messages-schema.json');
const validateSchema = require('../_helpers/validate-schema.js')(messagesSchema);

const status = require('../_helpers/status');

let core;
let mockMeta;
let mockTaksa;

beforeEach(() => {
    mockMeta = jest.fn();
    mockTaksa = jest.fn();
    core = {
        params: {
            withWidgets: '1'
        },
        service: (service) => service === 'taksa' ? mockTaksa : mockMeta,
        request: jest.fn().mockResolvedValue(filteredLabelsMock),
        console: {
            error: jest.fn()
        },
        yasm: {
            sum: jest.fn()
        },
        getServiceOptions: jest.fn(),
        res: {}
    };
    core.status = status(core);
});

test('-> PERM_FAIL без requests', async () => {
    const res = await method(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('no requests provided');
});

test('-> PERM_FAIL с невалидным requests', async () => {
    core.params.requests = { foo: 'bar' };

    const res = await method(core);

    expect(res.status.status).toBe(3);
    expect(res.status.phrase).toInclude('invalid requests schema');
});

describe('одиночный запрос', () => {
    beforeEach(() => {
        core.params.requests = [
            {
                fid: '1',
                since: '123456'
            }
        ];
    });

    it('happy path', async () => {
        core.request.mockResolvedValueOnce(filteredLabelsMock);
        mockMeta.mockResolvedValueOnce(messagesByFolderMock);
        mockTaksa.mockResolvedValueOnce(taksaMock);

        const res = await method(core);

        await validateSchema(res);
        expect(res).toMatchSnapshot();
    });
});
