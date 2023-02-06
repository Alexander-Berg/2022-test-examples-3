import { assert } from 'chai';
import { spy } from 'sinon';

import { Sender } from './Sender';
import {
    BaobabLogEventType,
    BaobabSendWay,
    IClientActionParams,
    ISender,
    ISendEventData,
    ISendNode,
    ITreeUpdateParams,
    VisibilityChangeType,
} from './Sender.typings/Sender';

// TODO: все тесты не работали, нужно написать новые

let sender: ISender;
const testNode: ISendNode = {
    id: '0',
    name: 'root',
    attrs: { service: 'test', ui: 'unit-test' },
    children: [
        { id: '1', name: 'child_1', attrs: { test: 'data' } },
        { id: '2', name: 'child_2' },
    ],
};
const actionParam: IClientActionParams = {
    node: testNode,
    eventData: { event: BaobabLogEventType.Click, cts: 0, eventId: 'xf0' },
};
const treeUpdateParam: ITreeUpdateParams = {
    node: testNode,
    eventData: { cts: 0, triggerEventTrusted: false, triggerEvent: undefined },
};
const actionBigParam: IClientActionParams = {
    ...actionParam,
    eventData: {
        ...actionParam.eventData,
        data: { longStr: 'x'.repeat(70000) },
    },
};
const showEvent = {
    event: BaobabLogEventType.Create,
    tree: testNode,
    cts: 0,
    'trigger-event-id': null,
    'trigger-event-trusted': false,
};
const appendEvent = {
    event: BaobabLogEventType.Append,
    tree: testNode,
    cts: 0,
    'trigger-event-id': null,
    'trigger-event-trusted': false,
};
const removeEvent = {
    event: BaobabLogEventType.Visibility,
    type: VisibilityChangeType.Hide,
    'block-id': testNode.id,
    cts: 0,
    'trigger-event-id': null,
    'trigger-event-trusted': false,
};
const attachEvent = {
    event: BaobabLogEventType.Visibility,
    type: VisibilityChangeType.Show,
    'block-id': testNode.id,
    cts: 0,
    'trigger-event-id': null,
    'trigger-event-trusted': false,
};
const clickEvent = {
    event: BaobabLogEventType.Click,
    id: testNode.id,
    service: actionParam.eventData.service,
    subservice: actionParam.eventData.subservice,
    'event-id': actionParam.eventData.id,
    cts: actionParam.eventData.cts,
    data: actionParam.eventData.data,
    behaviour: actionParam.eventData.behaviour,
};
const clickBigEvent = {
    event: BaobabLogEventType.Click,
    id: testNode.id,
    service: actionBigParam.eventData.service,
    subservice: actionBigParam.eventData.subservice,
    'event-id': actionBigParam.eventData.id,
    cts: actionBigParam.eventData.cts,
    data: actionBigParam.eventData.data,
    behaviour: actionBigParam.eventData.behaviour,
};
const imgSend = { way: BaobabSendWay.Img, success: true };
const baobabSend = { way: BaobabSendWay.SendBeacon, success: true };
const ajaxSend = { way: BaobabSendWay.Ajax, success: true, status: 200 };

class XMLHttpRequest {
    static DONE = 4;

    callback?: Function;
    status = 200;
    readyState = 4;

    open() {}
    addEventListener(_name: string, callback: Function) { this.callback = callback }
    send() { this.callback && this.callback() }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function checkEventCallback(callbackArgs: any[], eventData: ISendEventData, sentInfo: any) {
    assert.equal(callbackArgs.length, 1);
    const eventInfo = callbackArgs[0];

    assert.containsAllKeys(eventInfo, ['eventData', 'sendInfo'], 'Неверный формат ответа!');
    assert.deepEqual(eventInfo.eventData, eventData, 'Неверная отправленые данные');
    assert.equal(eventInfo.sendInfo.status, sentInfo.status, 'Неверная отправленые данные об отпралении');
    assert.equal(eventInfo.sendInfo.success, sentInfo.success, 'Неверная отправленые данные об отпралении');
    assert.equal(eventInfo.sendInfo.way, sentInfo.way, 'Неверная отправленые данные об отпралении');
}

describe('Sender', () => {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    let globalNavigator: any;
    let globalDocument: any;
    let globalXMLHttpRequest: any;
    /* eslint-enable @typescript-eslint/no-explicit-any */

    beforeEach(() => {
        sender = new Sender({
            hrefPrefix: 'https://test.com/clck/click/table=test',
            hrefPostfix: 'yandex/test',
        });

        // @ts-ignore
        globalNavigator = global.navigator;
        // @ts-ignore
        globalXMLHttpRequest = global.XMLHttpRequest;
        // @ts-ignore
        globalDocument = global.document;
        // @ts-ignore
        global.navigator = { userAgent: '', sendBeacon: spy(() => true) };
        // @ts-ignore
        global.XMLHttpRequest = XMLHttpRequest;
    });

    afterEach(() => {
        // @ts-ignore
        global.navigator = globalNavigator;
        // @ts-ignore
        global.document = globalDocument;
        // @ts-ignore
        global.XMLHttpRequest = globalXMLHttpRequest;
    });

    describe('base sends', () => {
        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should send created tree', () => {
            return new Promise(resolve => sender.createTree(treeUpdateParam, resolve))
                .then((...args) => checkEventCallback(args, showEvent, ajaxSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should send appended tree', () => {
            return new Promise(resolve => sender.appendTree(treeUpdateParam, resolve))
                .then((...args) => checkEventCallback(args, appendEvent, ajaxSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should send removed tree', () => {
            return new Promise(resolve => sender.detachTree(treeUpdateParam, resolve))
                .then((...args) => checkEventCallback(args, removeEvent, baobabSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should send attach tree', () => {
            return new Promise(resolve => sender.attachTree(treeUpdateParam, resolve))
                .then((...args) => checkEventCallback(args, attachEvent, baobabSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should send client action', () => {
            return new Promise(resolve => sender.clientAction(actionParam, resolve))
                .then((...args) => checkEventCallback(args, clickEvent, baobabSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should use one send for few request ', () => {
            return new Promise(resolve => {
                let i = 5;
                while (i--) sender.clientAction(actionParam, resolve);
            })
            // @ts-ignore
                .then(() => assert.deepEqual(navigator.sendBeacon.callCount, 1));
        });
    });

    describe('alternative sends', () => {
        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should use HTML element img if sendBeacon is disabled', async() => {
            // @ts-ignore
            global.navigator = { userAgent: '' };
            // @ts-ignore
            global.document = {
                // @ts-ignore
                createElement: (tag: string) => {
                    assert.equal(tag.toLowerCase(), 'img', 'created not Image tag');
                    return {
                        addEventListener: (name: string, callback: Function) => {
                            if (name === 'load') callback();
                        },
                    };
                },
            };
            const result = new Promise(resolve => sender.clientAction(actionParam, resolve))
                .then((...args) => checkEventCallback(args, clickEvent, imgSend));

            await result;

            return result;
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should use AJAX for long request with Chrome', () => {
            // @ts-ignore
            global.navigator = { userAgent: 'Chrome' };
            return new Promise(resolve => sender.clientAction(actionBigParam, resolve))
                .then((...args) => checkEventCallback(args, clickBigEvent, ajaxSend));
        });

        // eslint-disable-next-line mocha/no-skipped-tests
        it.skip('should use 2 sends for 43+ requests ', () => {
            return new Promise(resolve => {
                let i = 43;
                while (i--) sender.clientAction(actionParam, resolve);
            })
                // @ts-ignore
                .then(() => assert.deepEqual(navigator.sendBeacon.callCount, 2));
        });
    });
});
