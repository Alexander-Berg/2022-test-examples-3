'use strict';

const appendSmartReplies = require('./append-smart-replies.js');
const deepFreeze = require('deep-freeze');

let core;
let mockService;
let mockRequest;

const mockMsrData = {
    items: [
        {
            id: 'Ваше письмо получено, но еще не прочитано.',
            rank: 1.53819,
            hash: 'ваш еще не но письмо получать прочитывать',
            text: 'Ваше письмо получено, но еще не прочитано.'
        },
        {
            id: 'Большое спасибо за полезную информацию.',
            rank: 1.31925,
            hash: 'большой за информация полезный спасибо',
            text: 'Большое спасибо за полезную информацию.'
        },
        {
            id: 'Спасибо за интересную информацию.',
            rank: 1.30126,
            hash: 'за интересный информация спасибо',
            text: 'Спасибо за интересную информацию.'
        }
    ]
};

const mockEnvelopes = [
    { mid: '1', subjectInfo: { prefix: '', subject: 'subj 1' }, labels: [], types: [ '4' ] },
    { mid: '2', subjectInfo: { prefix: '', subject: 'subj 2' }, labels: [], types: [ '5' ] },
    { mid: '3', subjectInfo: { prefix: 're:', subject: 'subj 3' }, labels: [], types: [ '6', '72' ] },
    { mid: '5', subjectInfo: { prefix: 're:', subject: 'subj 5' }, labels: [], types: [ '4' ] }
];

beforeEach(() => {
    mockService = jest.fn();
    mockRequest = jest.fn();
    core = {
        params: {
            mids: '1,2,3'
        },
        config: {
            connectionId: 'TEST_CONNECTION_ID'
        },
        service: () => mockService,
        request: mockRequest
    };
});

test('без withSmartReplies не меняет bodies', async () => {
    const bodies = [ {} ];
    deepFreeze(bodies);

    await appendSmartReplies(core, bodies);

    expect(mockService).not.toBeCalled();
});

test('happy path', async () => {
    core.params.withSmartReplies = '1';
    const bodies = [
        { info: { mid: '1' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '2' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '3' }, body: [ { lang: 'ru' } ] },
        { status: { status: 3, phrase: 'PERM_FAIL message_body: unknown mid' } },
        { info: { mid: '4' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '5' }, body: [] }
    ];

    mockService.mockResolvedValueOnce({
        envelopes: mockEnvelopes
    });
    mockService.mockResolvedValue(mockMsrData);

    mockRequest.mockRejectedValueOnce({});

    await appendSmartReplies(core, bodies);

    expect(bodies).toMatchSnapshot();
});

test('параметры, улетающие в сервисы, правильные', async () => {
    core.params.withSmartReplies = '1';
    core.params.maxReplyLength = '1,2,3';
    const bodies = [
        { info: { mid: '1' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '2' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '3' }, body: [ { lang: 'ru', facts: '{"snippet":{"text": "text snippet 3"}}' } ] }
    ];

    mockService.mockResolvedValueOnce({
        envelopes: mockEnvelopes
    });
    mockService.mockResolvedValue(mockMsrData);
    mockRequest.mockResolvedValueOnce({
        messagesSnippets: [
            { text: 'iex snippet 1' },
            { text: 'iex snippet 2' },
            {}
        ]
    });

    await appendSmartReplies(core, bodies);

    expect(mockService.mock.calls).toMatchSnapshot();
});

test('если мета не ответила, не страшно, вернем bodies как есть', async () => {
    core.params.withSmartReplies = '1';
    const bodies = [
        { info: { mid: '1' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '2' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '3' }, body: [ { lang: 'ru' } ] }
    ];
    deepFreeze(bodies);

    mockService.mockRejectedValue({});
    mockRequest.mockRejectedValueOnce({});

    await appendSmartReplies(core, bodies);

    expect(bodies).toMatchSnapshot();
});

test('если msr не ответил, не страшно, вернем bodies как есть', async () => {
    core.params.withSmartReplies = '1';
    const bodies = [
        { info: { mid: '1' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '2' }, body: [ { lang: 'ru' } ] },
        { info: { mid: '3' }, body: [ { lang: 'ru' } ] }
    ];
    deepFreeze(bodies);

    mockService.mockResolvedValueOnce({
        envelopes: mockEnvelopes
    });
    mockService.mockRejectedValue({});
    mockRequest.mockRejectedValueOnce({});

    await appendSmartReplies(core, bodies);

    expect(bodies).toMatchSnapshot();
});
