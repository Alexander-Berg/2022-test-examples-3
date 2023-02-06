'use strict';

const diffset = require('./../../build/fetchers/diffset/diffset').default;
const { ChangeType } = require('./../../build/fetchers/diffset/change-types');
const { DiffActionType } = require('./../../build/fetchers/diffset/diff-action-type');
const { DiffEntityType } = require('./../../build/fetchers/diffset/diff-entity-type');
const { Thread } = require('./../../build/entities/thread');
const { DiffResponse } = require('./../../build/fetchers/diffset/diff-response');
const { DiffParams } = require('./../../build/fetchers/diffset/diff-params');
const { DiffExtra } = require('./../../build/fetchers/diffset/diff-extra');
const { Label, LabelDetails, LabelType } = require('./../../build/entities/label');
const { Folder, FolderDetails, FolderType } = require('./../../build/entities/folder');
const { Message, MessageDetails, Attachment, Recipient } = require('./../../build/entities/message');
const ApiError = require('../../../../routes/helpers/api-error');

let core;
let mainService;
let labelsService;
let successResponse;
let labelsResponse;
let error5001;
let error5013;

beforeEach(() => {
    mainService = jest.fn();
    labelsService = jest.fn();

    core = {
        params: {
            revision: 1234
        },
        service: () => (method, ...args) => (
            method === '/v2/changes' ?
                mainService(method, ...args) :
                labelsService(method, ...args)
        ),
        extra: {
            limit: 10
        }
    };
    // this.sinon.stub(core.services, 'meta')
    //     .withArgs(core, '/v2/changes').callsFake(mainService)
    //     .withArgs(core, '/labels').callsFake(labelsService);  // labels
    successResponse = {
        changes: [ {
            revision: 4321,
            type: 'update',
            value: [
                { mid: '1000', labels: [ '2000', '3000' ] }
            ]
        } ]
    };
    labelsResponse = {
        labels: {
            1: {
                symbolicName: { title: 'seen_label' }
            },
            fake_seen_label: {
                symbolicName: { title: 'seen_label' }
            },
            3: {
                symbolicName: { title: 'normal' }
            }
        }
    };
    error5001 = {
        error: {
            error: {
                code: 5001,
                message: 'invalid argument',
                reason: 'invalid max_count argument'
            }
        }
    };
    error5013 = {
        error: {
            error: {
                code: 5013,
                message: 'the revision can not be found',
                reason: 'revision has not been found in changelog'
            }
        }
    };
});

test('should convert parameters before calling meta', () => {
    mainService.mockResolvedValue(successResponse);
    labelsService.mockResolvedValue(labelsResponse);

    return diffset(core).get(new DiffParams(1234), new DiffExtra(10)).then(() => {
        expect(mainService).toHaveBeenCalledWith('/v2/changes', { revision: 1234, max_count: 10 });
        expect(labelsService).toHaveBeenCalledWith('/labels', {});
        const xivaParams = mainService.mock.calls[0][1];
        expect(xivaParams.revision).toBe(1234);
        expect(xivaParams.max_count).toBe(10);
    });
});

test('should convert the response', async () => {
    mainService.mockResolvedValueOnce(successResponse);
    labelsService.mockResolvedValueOnce(labelsResponse);
    const response = await diffset(core).get(new DiffParams(1234), new DiffExtra(10));
    expect(response).toEqual([ {
        action: DiffActionType.update,
        entity: DiffEntityType.message,
        revision: 4321,
        folders: null,
        labels: null,
        threads: null,
        messages: [ {
            id: '1000',
            details: {
                fid: null,
                tid: null,
                lids: [
                    '2000',
                    '3000'
                ],
                isEmptySubject: null,
                subjectPrefix: null,
                subject: null,
                isUnread: true,
                timestamp: null,
                attachments: null,
                firstline: null,
                from: null,
                to: null,
                cc: null,
                bcc: null,
                tab: null
            }
        } ]
    } ]);
});

test('converts message-related responses', async () => {
    const successResponse = {
        changes: [ {
            revision: 4321,
            type: ChangeType.store,
            value: [
                {
                    mid: '1000',
                    attachments: [ {
                        m_contentType: 'CONTENT TYPE',
                        m_fileName: 'FILE NAME',
                        m_hid: 'HID',
                        m_size: 12345
                    } ],
                    attachmentsCount: 1,
                    attachmentsFullSize: 12345,
                    bcc: [ {
                        displayName: 'BCC REC',
                        domain: 'yandex.com',
                        local: 'bcc'
                    } ],
                    cc: [ {
                        displayName: 'CC REC',
                        domain: 'yandex.com',
                        local: 'cc'
                    } ],
                    date: 123456,
                    fid: '112233',
                    firstline: 'FIRST LINE',
                    from: [ {
                        displayName: 'FROM REC',
                        domain: 'yandex.com',
                        local: 'from'
                    } ],
                    imapId: '54321',
                    inReplyTo: 'replyTo@yandex.com',
                    labels: [ '1', '10002' ],
                    newCount: 1,
                    receiveDate: 654321,
                    references: 'ref1',
                    replyTo: [ {
                        displayName: 'replyTo REC',
                        domain: 'yandex.com',
                        local: 'reply'
                    } ],
                    revision: 12345,
                    rfcId: 'rfcid',
                    size: 2233,
                    stid: 'stid',
                    subject: 'THE SUBJECT',
                    subjectInfo: {
                        type: 'type',
                        prefix: 'prefix',
                        postfix: 'postfix',
                        subject: 'subject',
                        isSplitted: true
                    },
                    threadCount: 1,
                    threadId: '112233',
                    to: [ {
                        displayName: 'to REC',
                        domain: 'yandex.com',
                        local: 'to'
                    } ],
                    types: [ 1, 2, 3 ],
                    uidl: 'uidl'
                },
                {
                    mid: '2000',
                    attachments: [],
                    attachmentsCount: 0,
                    attachmentsFullSize: 0,
                    bcc: [],
                    cc: [],
                    date: 123456,
                    fid: '123',
                    firstline: 'FIRST LINE 1',
                    from: [ {
                        displayName: 'FROM REC',
                        domain: 'yandex.com',
                        local: 'from'
                    } ],
                    imapId: '54321',
                    inReplyTo: 'replyTo@yandex.com',
                    labels: [ '1001' ],
                    newCount: 1,
                    receiveDate: 123456,
                    references: 'ref1',
                    replyTo: [],
                    revision: 12345,
                    rfcId: 'rfcid',
                    size: 2233,
                    stid: 'stid',
                    subject: 'THE SUBJECT',
                    subjectInfo: {
                        type: 'type',
                        prefix: 'p1',
                        postfix: 'p2',
                        subject: 'No subject',
                        isSplitted: true
                    },
                    threadCount: 1,
                    threadId: '112233',
                    to: [ {
                        displayName: 'to REC',
                        domain: 'yandex.com',
                        local: 'to'
                    } ],
                    types: [],
                    uidl: 'uidl'
                }
            ]
        } ]
    };

    mainService.mockResolvedValueOnce(successResponse);
    labelsService.mockResolvedValueOnce(labelsResponse);
    const response = await diffset(core).get(new DiffParams(1234), new DiffExtra(10));
    expect(response)
        .toEqual(
            [ new DiffResponse(DiffEntityType.draft, DiffActionType.update, 4321, null, null, null, [
                new Message(
                    '1000',
                    new MessageDetails(
                        '112233',
                        '112233',
                        [ '1', '10002' ],
                        false,
                        'prefix',
                        'subject',
                        false,
                        654321,
                        [ new Attachment(
                            'CONTENT TYPE',
                            'FILE NAME',
                            'HID',
                            12345
                        ) ],
                        'FIRST LINE',
                        new Recipient('from@yandex.com', 'FROM REC'),
                        [ new Recipient('to@yandex.com', 'to REC') ],
                        [ new Recipient('cc@yandex.com', 'CC REC') ],
                        [ new Recipient('bcc@yandex.com', 'BCC REC') ],
                        null
                    )
                ),
                new Message(
                    '2000',
                    new MessageDetails(
                        '123',
                        '112233',
                        [ '1001' ],
                        true,
                        'p1',
                        null,
                        true,
                        123456,
                        [],
                        'FIRST LINE 1',
                        new Recipient('from@yandex.com', 'FROM REC'),
                        [ new Recipient('to@yandex.com', 'to REC') ],
                        [],
                        [],
                        null
                    )
                )
            ]) ]);
});

test('converts folder-related responses', async () => {
    const successResponse = {
        changes: [ {
            revision: 4321,
            type: ChangeType.folderCreate,
            value: [
                {
                    id: '1000',
                    name: 'FOLDER NAME 1',
                    isThreadable: true,
                    messagesCount: 112233,
                    creationTime: 11223344,
                    newMessagesCount: 1122,
                    parentId: '321',
                    pop3on: 'true',
                    position: 2,
                    revision: 12345,
                    symbol: 'archive',
                    type: 'system',
                    unvisited: true
                }
            ]
        }, {
            revision: 6543,
            type: ChangeType.folderModify,
            value: [
                {
                    id: '2000',
                    name: 'FOLDER NAME 2',
                    isThreadable: false,
                    messagesCount: 123,
                    creationTime: 44332211,
                    unreadMessagesCount: 12,
                    parentId: '2',
                    pop3on: 'false',
                    position: 3,
                    revision: 54321,
                    symbol: 'outbox',
                    type: 'user',
                    unvisited: false
                }
            ]
        } ]
    };

    mainService.mockResolvedValueOnce(successResponse);
    labelsService.mockResolvedValueOnce(labelsResponse);
    const response = await diffset(core).get(new DiffParams(1234), new DiffExtra(10));
    expect(response).toEqual([
        new DiffResponse(DiffEntityType.folder, DiffActionType.create, 4321, [
            new Folder(
                '1000',
                new FolderDetails(
                    FolderType.archive,
                    'FOLDER NAME 1',
                    112233,
                    1122,
                    '321',
                    2
                )
            ) ], null, null, null
        ),
        new DiffResponse(DiffEntityType.folder, DiffActionType.update, 6543, [
            new Folder(
                '2000',
                new FolderDetails(
                    FolderType.user,
                    'FOLDER NAME 2',
                    123,
                    12,
                    '2',
                    3
                )
            ) ], null, null, null
        )
    ]);
});

test('converts label-related responses', async () => {
    const successResponse = {
        changes: [ {
            revision: 4321,
            type: ChangeType.labelCreate,
            value: [
                {
                    lid: '1000',
                    name: 'LABEL NAME 1',
                    messagesCount: 123,
                    creationTime: 11223344,
                    revision: 12345,
                    symbol: 'answered_label',
                    type: 'user',
                    color: 'red'
                }
            ]
        }, {
            revision: 1234,
            type: ChangeType.labelModify,
            value: [
                {
                    lid: '2000',
                    name: 'LABEL NAME 2',
                    messagesCount: 456,
                    creationTime: 11223344,
                    revision: 12345,
                    symbol: 'important_label',
                    type: 'system',
                    color: 'blue'
                }
            ]
        } ]
    };

    mainService.mockResolvedValueOnce(successResponse);
    labelsService.mockResolvedValueOnce(labelsResponse);
    const response = await diffset(core).get(new DiffParams(1234), new DiffExtra(10));
    expect(response).toEqual([
        new DiffResponse(DiffEntityType.label, DiffActionType.create, 4321, null, [ new Label(
            '1000',
            new LabelDetails(
                LabelType.user,
                'LABEL NAME 1',
                'red',
                123
            )
        ) ], null, null),
        new DiffResponse(DiffEntityType.label, DiffActionType.update, 1234, null, [ new Label(
            '2000',
            new LabelDetails(
                LabelType.important,
                'LABEL NAME 2',
                'blue',
                456
            )
        ) ], null, null)
    ]);
});

test('converts thread-related responses', async () => {
    const successResponse = {
        changes: [ {
            revision: 4321,
            type: ChangeType.threadsJoin,
            value: [
                {
                    mid: '1000',
                    tid: '2000',
                    labels: [ '3000', '4000' ]
                }
            ]
        } ]
    };

    mainService.mockResolvedValueOnce(successResponse);
    labelsService.mockResolvedValueOnce(labelsResponse);
    const response = await diffset(core).get(new DiffParams(1234), new DiffExtra(10));
    expect(response).toEqual([ new DiffResponse(
        DiffEntityType.thread,
        DiffActionType.update,
        4321, null, null, [
            new Thread('2000', '1000', [ '3000', '4000' ])
        ], null) ]);
});

test('should respond with error 500 if the main service fails', async () => {
    mainService.mockRejectedValueOnce();
    labelsService.mockResolvedValueOnce(labelsResponse);
    try {
        await diffset(core).get({}, {});
        return Promise.reject('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});

test('should respond with error 400 if the main service responds with code 5001', async () => {
    mainService.mockRejectedValueOnce(error5001);
    labelsService.mockResolvedValueOnce(labelsResponse);
    try {
        await diffset(core).get({}, {});
        return Promise.reject('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});

test('should respond with error 400 if the main service responds with code 5013', async () => {
    mainService.mockRejectedValueOnce(error5013);
    labelsService.mockResolvedValueOnce(labelsResponse);
    try {
        await diffset(core).get({}, {});
        return Promise.reject('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(400);
    }
});

test('should respond with error 500 if the labels service fails', async () => {
    mainService.mockResolvedValueOnce({
        changes: [ {
            revision: 4321,
            type: ChangeType.threadsJoin,
            value: [
                {
                    mid: '1000',
                    tid: '2000',
                    labels: [ '3000', '4000' ]
                }
            ]
        } ]
    });
    labelsService.mockRejectedValueOnce();
    try {
        await diffset(core).get({}, {});
        return Promise.reject('MUST REJECT');
    } catch (err) {
        expect(err).toBeInstanceOf(ApiError);
        expect(err.code).toBe(500);
    }
});
