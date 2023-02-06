import * as React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import { StaticRouter } from 'react-router-dom';
import getStore from '../../redux';

import * as metrika from '../../lib/metrika';
import { initRoutingVariables } from '../../helpers/routingVars';
import CommonContext, { CommonContextType } from '../../context/common';

import mockI18N from '../../__mocks__/i18n';
import getMockLocation from '../../__mocks__/location';

import { Fact, RainReport, TempRange } from '../../types';
import { MapCoords } from '../Map/useMap';
import { ReportState } from '../../redux/slices/report';
import dict from './i18n/ru';

import PrecipitationReportV2 from '.';

jest.unmock('react');
jest.mock('../../lib/rum');
jest.unmock('react-redux');

const mockLocation = getMockLocation();
const metrikaGoalSpy = jest.spyOn(metrika, 'reachGoal');

mockI18N('ru', dict);

describe('components', () => {
    describe('PrecipitationReport', () => {
        const coords = [50, 50] as MapCoords;
        const baseFact = { temp: 5, daytime: 'n' } as Fact;
        const baseReport = {
            tempRange: 'overcast',
            yesGoal: 'main_feedback.overcast.true',
            noGoal: 'main_feedback.overcast.false',
            goalParams: { region_id: 54, condition: 'overcast', real: undefined },
            collapsed: false,
            backendReportParams: { lat: 55.763069, lon: 37.967194 },
        } as RainReport;

        // @ts-ignore
        let store: ReturnType<typeof getStore> = null;

        // @ts-ignore
        let component: ReturnType<typeof mount> = null;

        const contextProvider = (context: Partial<CommonContextType>): React.FC => ({ children }) =>
            <Provider store={store}>
                <CommonContext.Provider value={context as CommonContextType}>
                    <StaticRouter location={context.routingVariables?.location}>
                        {children}
                    </StaticRouter>
                </CommonContext.Provider>
            </Provider>;

        const routingVariables = initRoutingVariables({ location: { ...mockLocation } });

        describe.each([
            ['Репорт для солнечной погоды в межсезонье днем', {
                fact: { ...baseFact, temp: 1, daytime: 'd' },
                report: { ...baseReport, tempRange: TempRange.Clear },
                goalParams: { real: 'overcast' },
                clarify: [
                    { icon: 'bknd' },
                    { icon: 'ovc' },
                    { icon: 'ovcrasn' },
                ],
            }],
            ['Репорт для пасмурной погоды летом днем', {
                fact: { ...baseFact, temp: 10, daytime: 'n' },
                report: { ...baseReport },
                goalParams: { real: 'partly' },
                clarify: [
                    { icon: 'skcn' },
                    { icon: 'bknn' },
                    { icon: 'ovcra' },
                ],
            }],
            ['Репорт для облачной погоды зимой ночью', {
                fact: { ...baseFact, temp: -3 },
                report: { ...baseReport, tempRange: TempRange.Overcast },
                goalParams: { real: 'partly' },
                clarify: [
                    { icon: 'skcn' },
                    { icon: 'bknn' },
                    { icon: 'ovcsn' },
                ],
            }],
            ['Репорт для дождливой погоды в межсезонье днем', {
                fact: { ...baseFact, temp: 1, daytime: 'd' },
                report: { ...baseReport, tempRange: TempRange.Prec },
                goalParams: { real: 'partly' },
                clarify: [
                    { icon: 'skcd' },
                    { icon: 'bknd' },
                    { icon: 'ovc' },
                ],
            }]
        ])('%s', (_, { fact, report, clarify, goalParams }) => {
            beforeEach(() => {
                // @ts-ignore
                store = getStore({ report: { reportState: ReportState.Shown } });
                component = mount(<PrecipitationReportV2 fact={fact} report={report} coords={coords} />, {
                    wrappingComponent: contextProvider({ routingVariables })
                });
            });

            it('Кнопка "да, верно" работает', () => {
                const buttonOk = component.find('.buttonOk');
                const popup = component.find('Popup');

                expect(popup.prop('visible')).toBeFalsy();
                buttonOk.simulate('click', {
                    currentTarget: {
                        value: buttonOk.prop('value'),
                    },
                });
                expect(metrikaGoalSpy).toHaveBeenLastCalledWith(`${baseReport.yesGoal}`, baseReport.goalParams);
                expect(popup.prop('visible')).toBeFalsy();
            });

            it('Кнопка "нет, не верно" работает', () => {
                expect(component.find('Popup').prop('visible')).toBeFalsy();
                component.find('.buttonNo').simulate('click');
                expect(component.find('.buttonNo').hasClass('buttonActive')).toBeTruthy();
                expect(metrikaGoalSpy).toHaveBeenLastCalledWith(`${report.noGoal}.choose`, report.goalParams);

                const popup = component.find('Popup');
                const buttons = popup.find('.clarify');

                expect(popup.prop('visible')).toBeTruthy();
                expect(buttons.length).toBe(3);

                const [b1, b2, b3] = [buttons.at(0), buttons.at(1), buttons.at(2)];

                expect(b1.find('Icon').prop('name')).toBe(clarify[0].icon);
                expect(b2.find('Icon').prop('name')).toBe(clarify[1].icon);
                expect(b3.find('Icon').prop('name')).toBe(clarify[2].icon);

                b2.simulate('click', {
                    currentTarget: {
                        value: report.noGoal,
                    },
                });

                expect(metrikaGoalSpy).toHaveBeenLastCalledWith(`${report.noGoal}`, { ...report.goalParams, ...goalParams });
                expect(component.find('Popup')).toMatchObject({});
            });
        });
    });
});
