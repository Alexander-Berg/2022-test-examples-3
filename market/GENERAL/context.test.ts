import {Context} from './context';

test('storeDebug. debug off', function () {
    const context = new Context(null, false);
    context.storeDebug({field: 'value'});

    expect(context.debugLog.length).toBe(0);
});

test('storeDebug. debug on', function () {
    const context = new Context(null, true);
    context.storeDebug({field: 'value'});

    expect(context.debugLog).toEqual([{field: 'value'}]);
});
