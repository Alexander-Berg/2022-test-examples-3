import * as React from 'react';
import { StaticRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import * as UrlManager from '../../lib/url-manager';
import { initRoutingVariables } from '../../helpers/routingVars';
import { settingsDeepLink } from '../../constants/weather-app';
import CommonContext, { CommonContextType } from '../../context/common';

import mockI18N from '../../__mocks__/i18n';
import getMockLocation from '../../__mocks__/location';

import locationRu from './i18n/ru';

import HeaderBase from './base';

const mockLocation = getMockLocation();

jest.unmock('react');
jest.mock('../../lib/rum');
jest.mock('../../lib/metrika');
jest.mock('@yandex-int/react-baobab-logger');

const isTurboAppAsWeatherApp = jest.spyOn(UrlManager, 'isTurboAppAsWeatherApp');

mockI18N('ru', locationRu);

describe('components', () => {
    describe('Header', () => {
        const provider = (context: Partial<CommonContextType>): React.FC => ({ children }) =>
            <CommonContext.Provider value={context as CommonContextType}>
                <StaticRouter>
                    {children}
                </StaticRouter>
            </CommonContext.Provider>;

        it('Настройки абсорбации', () => {
            isTurboAppAsWeatherApp.mockReturnValue(true);

            const routingVariables = initRoutingVariables({ location: { ...mockLocation } });

            const cmp = mount((
                <HeaderBase
                    logoIcon={<i />}
                />
            ), {
                wrappingComponent: provider({ routingVariables }),
            });

            const settings = cmp.find('.settings');

            expect(settings.name()).toBe('a');
            expect(settings.prop('href')).toBe(settingsDeepLink);

            isTurboAppAsWeatherApp.mockRestore();
        });
    });
});
