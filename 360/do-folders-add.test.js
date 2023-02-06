'use strict';

const model = require('./do-folders-add.js');

let core;
let mops;
let request;

beforeEach(() => {
    mops = jest.fn();
    request = jest.fn();
    core = {
        request: (model) => {
            if (model !== 'folders/touch') {
                return Promise.reject();
            }

            return request();
        },
        service: (name) => {
            if (name !== 'mops') {
                return jest.fn().mockRejectedValue();
            }

            return mops;
        }
    };
});

afterEach(() => {
    mops = request = undefined;
});

test('happy path', async () => {
    request.mockResolvedValue({
        folders: []
    });
    mops.mockResolvedValue({ fid: '42' });

    const res = await model({ folder_name: 'FAKE_NAME' }, core);

    expect(mops.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('folder exists', async () => {
    request.mockResolvedValue({
        folders: [
            {
                name: 'FAKE_NAME',
                fid: '54'
            }
        ]
    });
    mops.mockRejectedValue();

    const res = await model({ folder_name: 'FAKE_NAME' }, core);

    expect(res).toMatchSnapshot();
});

test('folder exists, different parents', async () => {
    request.mockResolvedValue({
        folders: [
            {
                name: 'FAKE_NAME',
                fid: '54'
            },
            {
                name: 'FAKE_NAME',
                parent_id: '1',
                fid: '55'
            }
        ]
    });
    mops.mockRejectedValue();

    const res = await model({ folder_name: 'FAKE_NAME', parent_id: 1 }, core);

    expect(res).toMatchSnapshot();
});
