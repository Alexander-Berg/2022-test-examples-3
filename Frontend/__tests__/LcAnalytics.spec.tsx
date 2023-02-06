import 'jest';
import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import lcEvents from '@yandex-turbo/components/LcEvents';
import { SwitchTriggers } from '@yandex-turbo/components/LcButton/LcButton.types';
import { LcEventAction, LcEventType, LcSectionType } from '@yandex-turbo/components/LcEvents/LcEvents.constants';
import { attachScriptNode } from '@yandex-turbo/components/lcUtils/attach-script-node';
import { asMock } from '@yandex-turbo/components/lcUtils/as-mock';

import { LcAnalytics } from '@yandex-turbo/components/LcAnalytics/LcAnalytics';
import { yanalyticsUrl, yandexMetrikaUrl } from '@yandex-turbo/components/LcAnalytics/LcAnalytics.constants';
import { AnalyticsAliases, ILcAnalyticsProps } from '@yandex-turbo/components/LcAnalytics/LcAnalytics.types';

import { analyticsMocks, presetMocks, trackGAParamsMock, visitParamsMock, bannerid } from './LcAnalytics.mocks';

jest.mock('../../lcUtils/attach-script-node');

const attachScriptNodeMock = asMock(attachScriptNode);
const commonEvent = {
    type: LcEventType.OnClick,
    target: { type: LcSectionType.LcAnalytics },
};

function flushPromises() {
    /*
        Асинхронность componentDidMount компонента создаёт микротаски.
        Чтобы дождаться их выполнения перед дальнейшим тестированием, создаём макротаск через setImmediate.
        https://medium.com/@lucksp_22012/jest-enzyme-react-testing-with-async-componentdidmount-7c4c99e77d2d
    */
    return new Promise(resolve => setImmediate(resolve));
}
async function renderLcAnalytics({ analytics = {}, analyticsPreset, bannerid }: ILcAnalyticsProps) {
    const wrapper = shallow<LcAnalytics>(
        <LcAnalytics analytics={analytics} analyticsPreset={analyticsPreset} bannerid={bannerid} />
    );

    await flushPromises();

    return wrapper;
}

describe('LcAnalytics', () => {
    let initSpy = jest.fn();
    let addSpy = jest.fn();
    let reachGoalSpy = jest.fn();
    let paramsSpy = jest.fn();
    let hitSpy = jest.fn();
    let cookieSpy = jest.fn();
    let wrapper: ShallowWrapper;

    beforeAll(() => {
        attachScriptNodeMock.mockResolvedValue(null);

        Object.defineProperty(window, 'Ya', {
            writable: true,
            value: {
                Metrika2: () => {
                    initSpy();

                    return {
                        reachGoal: reachGoalSpy,
                        params: paramsSpy,
                        hit: hitSpy,
                    };
                },
                yanalytics: {
                    add: addSpy,
                },
            },
        });

        Object.defineProperty(document, 'cookie', {
            set: cookieSpy,
        });
    });

    afterEach(() => {
        initSpy.mockReset();
        addSpy.mockReset();
        reachGoalSpy.mockReset();
        paramsSpy.mockReset();
        hitSpy.mockReset();
        cookieSpy.mockReset();
        attachScriptNodeMock.mockReset();
        wrapper.unmount();
    });

    describe('initialization', () => {
        let loadSpy = jest.fn();

        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
            loadSpy.mockReset();
        });

        it('should init analytics after the script was loaded', async() => {
            attachScriptNodeMock.mockReturnValueOnce(new Promise(resolve => {
                setTimeout(() => {
                    loadSpy();
                    resolve();
                }, 100);
            }));

            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            // Используем фейковые таймеры jest'а для проверки резолва промиса скрипта по таймауту
            jest.advanceTimersByTime(20);
            expect(loadSpy).not.toBeCalled();
            expect(initSpy).not.toBeCalled();

            jest.advanceTimersByTime(80);
            expect(loadSpy).toBeCalled();
            // Здесь промис загрузки скрипта ещё не выполнен, поэтому инициализация метрики не вызвана.
            expect(initSpy).not.toBeCalled();

            /*
                Асинхронный промис, который возвращает attachScriptNodeMock, тоже создаст микротаски.
                Необходимо дождаться их выполнения, чтобы убедиться, что инициализация аналитики происходит
                после загрузки скрипта.
            */
            await flushPromises();

            expect(initSpy).toBeCalled();
        });

        it('should listen to setBannerId event before script init', async() => {
            const lcEventsOnSpy = jest.spyOn(lcEvents, 'on');
            const lcEventsinitTriggerSpy = jest.spyOn(lcEvents, 'initTrigger');

            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const onOrder = lcEventsOnSpy.mock.invocationCallOrder[0];
            const initTriggerOrder = lcEventsinitTriggerSpy.mock.invocationCallOrder[0];
            expect(onOrder).toBeLessThan(initTriggerOrder);
        });
    });

    describe('add only the necessary scripts', () => {
        it('should add only metrika', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            expect(attachScriptNodeMock).toBeCalledWith(yandexMetrikaUrl, { async: true, crossOrigin: 'anonymous' });
        });

        it('should add only yanalytics', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.ga, bannerid });

            expect(attachScriptNodeMock).toBeCalledWith(yanalyticsUrl, { async: true, crossOrigin: 'anonymous' });
        });

        it('should add metrika and yanalytics', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrikaWithGA, bannerid });

            expect(attachScriptNodeMock).nthCalledWith(1, yandexMetrikaUrl, { async: true, crossOrigin: 'anonymous' });
            expect(attachScriptNodeMock).nthCalledWith(2, yanalyticsUrl, { async: true, crossOrigin: 'anonymous' });
        });
    });

    describe('track google analytics', () => {
        it('should replace url key with page', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrikaWithGA, bannerid });

            const event = { ...commonEvent, data: trackGAParamsMock.base, action: LcEventAction.TrackGAGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).toBeCalled();

            // первый вызов – инициализация, поэтому сравниваем со вторым
            const trackGACall = addSpy.mock.calls[1][0];

            expect(trackGACall).toMatchSnapshot();
        });

        it('should pass all defined params', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrikaWithGA, bannerid });

            const event = { ...commonEvent, data: trackGAParamsMock.full, action: LcEventAction.TrackGAGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).toBeCalled();

            // первый вызов – инициализация, поэтому сравниваем со вторым
            const mockCall = addSpy.mock.calls[1][0];

            expect(mockCall.params).toMatchSnapshot();
        });

        it('should not call window.Ya if there\'s no GA', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { ...commonEvent, data: trackGAParamsMock.base, action: LcEventAction.TrackGAGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).not.toBeCalled();
        });
    });

    describe('set visit params', () => {
        it('should set params', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { ...commonEvent, data: { params: visitParamsMock.base }, action: LcEventAction.SetSessionParameter };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(paramsSpy).toBeCalled();
            expect(paramsSpy).toBeCalledWith(visitParamsMock.base);
        });

        it('should set params for every counter', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, analyticsPreset: presetMocks.metrika, bannerid });

            const event = { ...commonEvent, data: { params: visitParamsMock.base }, action: LcEventAction.SetSessionParameter };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(paramsSpy).toBeCalledTimes(2);
        });

        it('should set bannerid', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { type: LcEventType.OnClick, target: { type: LcSectionType.LcPage }, data: {}, action: LcEventAction.SetBannerid };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(paramsSpy).toBeCalledTimes(1);
            expect(paramsSpy).toBeCalledWith({ bannerid });
        });

        it('should not set bannerid if there\'s no any', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika });

            const event = { type: LcEventType.OnClick, target: { type: LcSectionType.LcPage }, data: {}, action: LcEventAction.SetBannerid };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(paramsSpy).not.toBeCalled();
        });
    });

    describe('track page view', () => {
        it('should track GA with data', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.ga, bannerid });

            const event = { ...commonEvent, data: { alias: AnalyticsAliases.googleAnalytics, additional: 'data' }, action: LcEventAction.TrackPageView };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).toBeCalled();
            expect(addSpy.mock.calls).toMatchSnapshot();
        });

        it('should not be called for metrika', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { ...commonEvent, data: { alias: AnalyticsAliases.googleAnalytics, additional: 'data' }, action: LcEventAction.TrackPageView };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).not.toBeCalled();
        });
    });

    describe('track virtual page', () => {
        it('should track VP if there\'s an url', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const data = { url: 'url' };
            const event = { ...commonEvent, data, action: LcEventAction.HitVirtualPage };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(hitSpy).toBeCalledTimes(1);
            expect(hitSpy).toBeCalledWith('url');
        });

        it('should not called without url', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrikaWithGA, bannerid });

            const event = { ...commonEvent, data: {}, action: LcEventAction.HitVirtualPage };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(hitSpy).not.toBeCalled();
        });

        it('should work with yanalytics', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.full, bannerid });

            const data = { url: 'url' };
            const event = { ...commonEvent, data, action: LcEventAction.HitVirtualPage };
            lcEvents.execute(LcEventType.OnClick, event);

            // вычитаем 1 потому что в моке есть метрика
            const yanalyticsServicesCount = Object.keys(analyticsMocks.full).length - 1;

            expect(addSpy).toBeCalledTimes(yanalyticsServicesCount * 2); // первый вызов init, второй track
            expect(addSpy.mock.calls.slice(yanalyticsServicesCount)).toMatchSnapshot();
        });
    });

    describe('track switch goal', () => {
        it('should track goal if there\'s one in preset\'s counter', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.empty, analyticsPreset: presetMocks.metrikaWithGA, bannerid });

            const event = { ...commonEvent, data: { goal: SwitchTriggers.Click }, action: LcEventAction.ReachSwitchGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy).toBeCalledWith('switch', {});
        });

        it('should not track goal if there\'s no such goal in counter', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.empty, analyticsPreset: presetMocks.metrikaWithGA, bannerid });

            const event = { ...commonEvent, data: { goal: SwitchTriggers.Click }, action: LcEventAction.ReachSwitchGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(addSpy).toBeCalledTimes(1); // 1 – инициализация
        });

        it('should track goals if they are different between counters', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.empty, analyticsPreset: presetMocks.metrikaWithDifferentGoals, bannerid });

            const event = { ...commonEvent, data: { goal: SwitchTriggers.Click }, action: LcEventAction.ReachSwitchGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy).toBeCalledWith('switch', {});
            expect(reachGoalSpy).toBeCalledWith('another_switch', {});
        });
    });

    describe('reach goal', () => {
        it('should track goal', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });
            const event = { ...commonEvent, data: { goals: 'click' }, action: LcEventAction.ReachGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy).toBeCalledWith('click', { base: 'param' });
        });

        it('should not work without goals', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { ...commonEvent, data: {}, action: LcEventAction.ReachGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy).not.toBeCalled();
        });

        it('should not work without metrika counters', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.empty, bannerid });

            const event = { ...commonEvent, data: {}, action: LcEventAction.ReachGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy).not.toBeCalled();
        });

        it('should parse goals', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            const event = { ...commonEvent, data: { goals: 'click, hover,visit' }, action: LcEventAction.ReachGoal };
            lcEvents.execute(LcEventType.OnClick, event);

            expect(reachGoalSpy.mock.calls).toMatchSnapshot();
        });
    });

    describe('should set bannerid for custom scripts', () => {
        it('should write bannerid to ys cookie', async() => {
            wrapper = await renderLcAnalytics({ analytics: analyticsMocks.metrika, bannerid });

            lcEvents.execute(LcEventType.OnCustomScriptCall, {
                type: LcEventType.OnCustomScriptCall,
                action: LcEventAction.SetBannerid,
                target: {
                    type: LcSectionType.LcPage,
                },
                data: {},
            });

            expect(cookieSpy).toBeCalledWith('ys=bnrd.1234567890; path=/; domain=.yandex.undefined; secure');
        });
    });
});
