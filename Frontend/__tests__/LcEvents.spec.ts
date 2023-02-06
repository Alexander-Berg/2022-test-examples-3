import lcEvents from '../index';
import { LcSectionType, LcEventType, LcEventAction } from '../LcEvents.constants';

const eventHandlerSpy = jest.fn();

describe('lcEvents', () => {
    const target = { type: LcSectionType.LcGroup, sectionId: '123' };
    const action = 'action';

    beforeAll(() => {
        lcEvents.initTrigger(target, { handler: eventHandlerSpy });
    });

    beforeEach(() => {
        eventHandlerSpy.mockReset();
    });

    describe('lcEvents.initTrigger()', () => {
        test('should create and initialize trigger', () => {
            const type = LcSectionType.LcEmailSubscription;
            const sectionId = '123';

            lcEvents.initTrigger({ type, sectionId });

            const allTriggers = lcEvents.getTriggers();

            expect(allTriggers[type][sectionId]).toBeDefined();
            expect(allTriggers[type][sectionId].isInitiated).toBeTruthy();
        });

        test('should use type instead of sectionId if there if no sectionId', () => {
            const type = LcSectionType.LcFeatures;

            lcEvents.initTrigger({ type });

            const allTriggers = lcEvents.getTriggers();

            expect(allTriggers[type][type]).toBeDefined();
            expect(allTriggers[type][type].isInitiated).toBeTruthy();
        });
    });

    describe('lcEvents.removeTrigger()', () => {
        const target = { type: LcSectionType.LcEventsInfo, sectionId: '321' };

        test('should remove initialized trigger', () => {
            lcEvents.initTrigger(target);

            const allTriggers = lcEvents.getTriggers();

            expect(allTriggers[target.type][target.sectionId]).toBeDefined();
            lcEvents.removeTrigger(target);
            expect(allTriggers[target.type][target.sectionId]).not.toBeDefined();
        });

        test('should not remove uninitialized trigger', () => {
            lcEvents.initTrigger(target);

            const allTriggersBeforeRemove = lcEvents.getTriggers();
            lcEvents.removeTrigger({ type: LcSectionType.LcEventsSmallInfo });
            const allTriggersAfterRemove = lcEvents.getTriggers();

            expect(allTriggersBeforeRemove).toEqual(allTriggersAfterRemove);
        });
    });

    describe('lcEvents.handleEvent()', () => {
        test('should add trigger and keep it uninitialized', () => {
            const event = {
                type: LcEventType.OnPageLoad,
                target: { type: LcSectionType.LcAnalytics },
                data: { a: 1 },
            };
            lcEvents.execute(LcEventType.OnPageLoad, event);

            const allTriggers = lcEvents.getTriggers();

            expect(allTriggers[LcSectionType.LcAnalytics][LcSectionType.LcAnalytics]).toBeDefined();
            expect(allTriggers[LcSectionType.LcAnalytics][LcSectionType.LcAnalytics].inited).toBeFalsy();
        });
    });

    describe('lcEvents.execute()', () => {
        test('should call handler on Events.execute with eventType', () => {
            const event = { type: LcEventType.OnClick, target, data: { a: 1 }, action };

            lcEvents.execute(LcEventType.OnClick, event);

            expect(eventHandlerSpy).toHaveBeenCalledTimes(1);
            expect(eventHandlerSpy.mock.calls[0]).toEqual([event, {}]);
        });

        test('should call handler on Events.execute with options', () => {
            const event = { type: LcEventType.OnClick, target, data: { a: 1 }, action };

            lcEvents.execute(LcEventType.OnClick, event, { nativeEvent: 'event', nodeInfo: 'info' });

            expect(eventHandlerSpy).toHaveBeenCalledTimes(1);
            expect(eventHandlerSpy.mock.calls[0]).toEqual([event, { nativeEvent: 'event', nodeInfo: 'info' }]);
        });

        test('should call handler as many times as many events has been passed on Events.handle', () => {
            lcEvents.execute(LcEventType.OnClick, [
                { type: LcEventType.OnClick, target, data: { a: 1 }, action },
                { type: LcEventType.OnClick, target, data: { a: 2 }, action },
            ]);

            expect(eventHandlerSpy).toHaveBeenCalledTimes(2);
            expect(eventHandlerSpy.mock.calls[0]).toEqual([{ type: LcEventType.OnClick, target, data: { a: 1 }, action }, {}]);
            expect(eventHandlerSpy.mock.calls[1]).toEqual([{ type: LcEventType.OnClick, target, data: { a: 2 }, action }, {}]);
        });

        test('should call handler on Events.handle with eventType only for events with type', () => {
            lcEvents.execute(LcEventType.OnSubmit, [
                { type: LcEventType.OnClick, target, data: { a: 1 }, action },
                { type: LcEventType.OnSubmit, target, data: { a: 2 }, action },
                { type: LcEventType.OnClick, target, data: { a: 3 }, action },
            ]);

            expect(eventHandlerSpy).toHaveBeenCalledTimes(1);
            expect(eventHandlerSpy.mock.calls[0]).toEqual([{ type: LcEventType.OnSubmit, target, data: { a: 2 }, action }, {}]);
        });
    });

    describe('lcEvents.on', () => {
        test('should call callback after event executing', () => {
            const callbackSpy = jest.fn();
            const event = {
                action: LcEventAction.Open,
                type: LcEventType.OnClick,
                target,
                data: {},
            };

            lcEvents.on(target, LcEventAction.Open, callbackSpy);
            lcEvents.execute(LcEventType.OnClick, event);

            expect(callbackSpy).toBeCalled();
        });

        test('should complete all actions after event executing', () => {
            const openCallbackSpy = jest.fn();
            const closeCallbackSpy = jest.fn();
            const events = [
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
                { type: LcEventType.OnSubmit, action: LcEventAction.Close, data: {}, target },
                { type: LcEventType.OnClick, action: LcEventAction.Close, data: {}, target },
            ];

            lcEvents.on(target, LcEventAction.Open, openCallbackSpy);
            lcEvents.on(target, LcEventAction.Close, closeCallbackSpy);
            lcEvents.execute(LcEventType.OnClick, events);

            expect(openCallbackSpy).toHaveBeenCalledTimes(1);
            expect(closeCallbackSpy).toHaveBeenCalledTimes(1);
        });

        test('should not call callback for another event type', () => {
            const openCallbackSpy = jest.fn();
            const closeCallbackSpy = jest.fn();
            const events = [
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
                { type: LcEventType.OnSubmit, action: LcEventAction.Close, data: {}, target },
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
            ];

            lcEvents.on(target, LcEventAction.Open, openCallbackSpy);
            lcEvents.on(target, LcEventAction.Close, closeCallbackSpy);
            lcEvents.execute(LcEventType.OnSubmit, events);

            expect(openCallbackSpy).toHaveBeenCalledTimes(0);
            expect(closeCallbackSpy).toHaveBeenCalledTimes(1);
        });

        test('should call callback as many times as events has been passed', () => {
            const callbackSpy = jest.fn();
            const events = [
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
                { type: LcEventType.OnClick, action: LcEventAction.Open, data: {}, target },
            ];

            lcEvents.on(target, LcEventAction.Open, callbackSpy);
            lcEvents.execute(LcEventType.OnClick, events);

            expect(callbackSpy).toHaveBeenCalledTimes(3);
        });

        test('should add trigger if there is none', () => {
            const callbackSpy = jest.fn();
            const triggers = lcEvents.getTriggers();

            expect(triggers[LcSectionType.LcBadgeList]).toBeUndefined();

            lcEvents.on({ type: LcSectionType.LcBadgeList }, LcEventAction.Open, callbackSpy);

            expect(triggers[LcSectionType.LcBadgeList]).toBeDefined();
        });

        test('should add callback for not initialized trigger', () => {
            const callbackSpy = jest.fn();
            const triggers = lcEvents.getTriggers();
            const target = { type: LcSectionType.LcCustomJs, sectionId: '456' };

            expect(triggers[LcSectionType.LcCustomJs]).toBeUndefined();

            lcEvents.on(target, LcEventAction.Open, callbackSpy);

            lcEvents.initTrigger(target);

            lcEvents.execute(LcEventType.OnClick, {
                type: LcEventType.OnClick,
                action: LcEventAction.Open,
                target: { type: LcSectionType.LcCustomJs, sectionId: '456' },
                data: {},
            });

            expect(callbackSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('lcEvents.off', () => {
        test('should delete callback', () => {
            const callbackSpy = jest.fn();
            const callbackToDeleteSpy = jest.fn();
            const event = {
                type: LcEventType.OnClick,
                action: LcEventAction.Open,
                target,
                data: {},
            };

            lcEvents.on(target, LcEventAction.Open, callbackSpy);
            lcEvents.on(target, LcEventAction.Open, callbackToDeleteSpy);
            lcEvents.execute(LcEventType.OnClick, event);

            expect(callbackSpy).toHaveBeenCalledTimes(1);
            expect(callbackToDeleteSpy).toHaveBeenCalledTimes(1);

            lcEvents.off(target, LcEventAction.Open, callbackToDeleteSpy);

            lcEvents.execute(LcEventType.OnClick, event);

            expect(callbackSpy).toHaveBeenCalledTimes(2);
            expect(callbackToDeleteSpy).toHaveBeenCalledTimes(1);
        });

        test('should not delete callback for not initialized trigger', () => {
            const callbackSpy = jest.fn();
            const triggers = lcEvents.getTriggers();
            const sectionId = '456';
            const target = { type: LcSectionType.LcBodyImage, sectionId };
            const event = {
                type: LcEventType.OnClick,
                action: LcEventAction.Open,
                target,
                data: {},
            };

            expect(triggers[LcSectionType.LcBodyImage]).toBeUndefined();

            lcEvents.on(target, LcEventAction.Open, callbackSpy);
            lcEvents.off(target, LcEventAction.Open, callbackSpy);
            lcEvents.execute(LcEventType.OnClick, event);

            expect(callbackSpy).toHaveBeenCalledTimes(0);
        });
    });
});
