'use strict';

const labelsMock = require('../../test/mock/labels.json');
const labels = require('./labels.js');

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        service: () => mockService
    };
});

test('работает', async () => {
    mockService.mockResolvedValueOnce(labelsMock);

    const res = await labels({}, core);

    expect(res).toMatchSnapshot();
});
