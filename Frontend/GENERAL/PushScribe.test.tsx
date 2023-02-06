import React from 'react';
import ReactRouterDom from 'react-router-dom';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

import * as metrika from '../../lib/metrika';
import * as rum from '../../lib/rum';
import * as apiSup from '../../lib/apiSup';

import { initRoutingVariables } from '../../helpers/routingVars';
import CommonContext, { CommonContextType } from '../../context/common';

import getMockLocation from '../../__mocks__/location';
import PushScribe from './Component';

jest.unmock('react');
jest.mock('react-router-dom');
jest.mock('../../lib/rum');

const mockLocation = getMockLocation();
// eslint-disable-next-line react-hooks/rules-of-hooks
const mockHistory = ReactRouterDom.useHistory();
const editTagsSpy = jest.spyOn(apiSup, 'editTags');
const getUUIDSpy = jest.spyOn(apiSup, 'getUUID');
const logErrorSpy = jest.spyOn(rum, 'logError');
const metrikaGoalSpy = jest.spyOn(metrika, 'reachGoal');

describe('components', () => {
    describe('PushScribe', () => {
        const contextProvider = (context: Partial<CommonContextType>): React.FC => ({ children }) =>
            <CommonContext.Provider value={context as CommonContextType}>
                {children}
            </CommonContext.Provider>;

        const contolledPromise = () => {
            // eslint-disable-next-line
            let delayedResolver: () => void, delayedRejecter: (err: Error) => void;

            const promise = new Promise((resolve, reject) => {
                delayedResolver = resolve;
                delayedRejecter = reject;
            });

            // @ts-ignore
            return { promise, resolve: delayedResolver, reject: delayedRejecter };
        };

        // we need to update react to 1.6.9 at least
        // https://github.com/testing-library/react-hooks-testing-library/issues/14#issuecomment-475096681
        const consoleErr = console.error.bind(console);

        beforeEach(() => {
            metrikaGoalSpy.mockClear();
            global.console.error = () => {};
        });

        afterEach(async() => {
            mockHistory.location = mockLocation;
            await new Promise(resolve => {
                setTimeout(() => {
                    global.console.error = consoleErr;
                    resolve();
                }, 1);
            });
        });

        it('should not open modal without ?unpush', () => {
            const routingVariables = initRoutingVariables({ location: { ...mockLocation } });
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({ routingVariables }),
            });
            const modal = cmp.find('Modal');

            expect(modal.prop('visible')).toBeFalsy();
            expect(metrikaGoalSpy).toBeCalledTimes(0);
        });

        it('should open modal', async() => {
            const topic = 'weather_trend';
            const routingVariables = initRoutingVariables({ location: {
                ...mockLocation, search: `?unpush=${topic}`
            } });
            const uuid = 'some-uuid';
            getUUIDSpy.mockResolvedValueOnce(uuid);
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({ routingVariables }),
            });
            await new Promise(resolve => setTimeout(resolve, 10));
            const modal = cmp.find('Modal');

            expect(cmp.name()).toBe('PushScribe');

            expect(modal.prop('visible')).toBeTruthy();
            expect(modal.prop('className')).toMatch('Modal_theme_weather');
            expect(modal.prop('zIndex')).toBeGreaterThan(100);
            expect(modal.prop('onClose')).toBeInstanceOf(Function);

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.show', { topic, uuid });
        });

        it('should close modal', () => {
            const topic = 'weather_trend';
            const localMockLocation = { ...mockLocation, search: '?lat=55' };
            const routingVariables = initRoutingVariables({ location: {
                ...localMockLocation, search: `?lat=55&unpush=${topic}`
            } });
            mockHistory.location = localMockLocation;
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({ routingVariables }),
            });
            const buttons = cmp.find('.buttons');

            buttons.childAt(1).simulate('click');

            expect(buttons.children()).toHaveLength(2);
            expect(cmp.find('Modal').prop('visible')).toBeFalsy();
            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.hide', { topic });
            expect(mockHistory.replace).toHaveBeenLastCalledWith(localMockLocation);
        });

        it('should be ok confirm > error > retry > success', async() => {
            const topic = 'weather_trend';
            const routingVariables = initRoutingVariables({ location: {
                ...mockLocation, search: `?unpush=${topic}`
            } });
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({ routingVariables }),
            });
            await new Promise(resolve => setTimeout(resolve, 10));
            const getButtons = () => cmp.find('.buttons');
            const getBtn = (index = 0) => getButtons().childAt(index);
            const error = new Error('test error');

            // экран подтверждения, жмем "ок, отписаться"
            let promise = contolledPromise();

            editTagsSpy.mockReturnValueOnce(promise.promise);

            expect(getButtons().children()).toHaveLength(2);
            expect(getBtn().prop('disabled')).toBeFalsy();
            expect(getBtn(1).prop('disabled')).toBeFalsy();

            getBtn().simulate('click');

            expect(editTagsSpy).toHaveBeenLastCalledWith({ topics: [{ action: 'off', name: topic }] });
            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.btn.confirm', { topic });
            expect(getBtn().prop('disabled')).toBeTruthy();
            expect(getBtn(1).prop('disabled')).toBeTruthy();

            act(() => {
                // ведем на экран ошибки
                promise.reject(error);
            });

            await new Promise(resolve => setTimeout(resolve, 0));
            cmp.update();

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.error', { topic });
            expect(logErrorSpy).toHaveBeenLastCalledWith({
                additional: { topic: 'weather_trend' },
                block: 'PushScribe',
                level: 'warn',
                message: 'UnSubscribe error',
                method: 'operatePush'
            }, error);

            // экран ошибки, жмем "повторить"
            promise = contolledPromise();
            editTagsSpy.mockReturnValueOnce(promise.promise);

            getBtn().simulate('click');

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.btn.retry', { topic });
            expect(getBtn().prop('disabled')).toBeTruthy();

            act(() => {
                // снова ведем на экран ошибки
                promise.reject(error);
            });

            await new Promise(resolve => setTimeout(resolve, 0));
            cmp.update();

            expect(getBtn().prop('disabled')).toBeFalsy();
            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.error', { topic });

            // снова выполняем повтор, но уже ведем на экран успеха
            promise = contolledPromise();
            editTagsSpy.mockReturnValueOnce(promise.promise);

            getBtn().simulate('click');

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.btn.retry', { topic });
            expect(getBtn().prop('disabled')).toBeTruthy();

            act(() => {
                // ведем на экран успеха
                promise.resolve();
            });

            await new Promise(resolve => setTimeout(resolve, 0));
            cmp.update();

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.success', { topic });
        });

        it('should be ok confirm > success with settings', async() => {
            const topic = 'any_unsupported_topic';
            const routingVariables = initRoutingVariables({ location: {
                ...mockLocation, search: `?unpush=${topic}`
            } });
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({
                    routingVariables,
                    expFlags: { spa_pushscribe_settings: true }
                }),
            });
            await new Promise(resolve => setTimeout(resolve, 10));
            const getButtons = () => cmp.find('.buttons');
            const getBtn = (index = 0) => getButtons().childAt(index);

            // экран подтверждения, жмем "ок, отписаться"

            editTagsSpy.mockResolvedValueOnce(true);
            getBtn().simulate('click');

            await new Promise(resolve => setTimeout(resolve, 0));
            cmp.update();

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.success', { topic });
            expect(getButtons().children()).toHaveLength(2);

            getBtn(1).simulate('click');

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.btn.settings', { topic });

            getBtn().simulate('click');

            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.hide', { topic });
            expect(cmp.find('Modal').prop('visible')).toBeFalsy();
        });

        it('should be ok confirm > success without settings', async() => {
            const topic = 'any_unsupported_topic';
            const routingVariables = initRoutingVariables({ location: {
                ...mockLocation, search: `?unpush=${topic}`
            } });
            const cmp = mount(<PushScribe />, {
                wrappingComponent: contextProvider({
                    routingVariables,
                    expFlags: { spa_pushscribe_settings: false }
                }),
            });
            const getButtons = () => cmp.find('.buttons');
            const getBtn = (index = 0) => getButtons().childAt(index);

            // экран подтверждения, жмем "ок, отписаться"

            editTagsSpy.mockResolvedValueOnce(true);
            getBtn().simulate('click');

            await new Promise(resolve => setTimeout(resolve, 0));
            cmp.update();

            expect(getButtons().children()).toHaveLength(1);

            getBtn().simulate('click');

            const metrikaButtonCall = metrikaGoalSpy.mock.calls.length - 1;

            expect(metrikaGoalSpy).toHaveBeenNthCalledWith(metrikaButtonCall, 'pushScribe.btn.close', { topic });
            expect(metrikaGoalSpy).toHaveBeenLastCalledWith('pushScribe.hide', { topic });
            expect(cmp.find('Modal').prop('visible')).toBeFalsy();
        });
    });
});
