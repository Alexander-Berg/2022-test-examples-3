import * as React from 'react';
import { shallow, mount } from 'enzyme';
import * as serializer from 'jest-serializer-html';

import lcEvents from '@yandex-turbo/components/LcEvents';
import { executeSubmitGoal } from '@yandex-turbo/components/LcEvents/LcEvents.helpers';
import { LcAlign, LcSizes } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcYandexFormComponent } from '../LcYandexForm';

expect.addSnapshotSerializer(serializer);

jest.mock('@yandex-turbo/components/LcEvents');
jest.mock('@yandex-turbo/components/LcEvents/LcEvents.helpers');

describe('LcYandexForm component', () => {
    const props = {
        align: LcAlign.LEFT,
        anchor: 'yandex-form-id',
        device: 'desktop',
        events: [],
        id: 'yandex-form-id',
        ignoreLang: false,
        isPreview: false,
        offsets: { padding: { top: LcSizes.NONE, bottom: LcSizes.S } },
        sectionId: 'yandex-form-id',
        useCustomProvider: false,
        autofill: false,
        params: [],
    };

    test('should execute events', () => {
        const instance = shallow<LcYandexFormComponent>(<LcYandexFormComponent {...props} />).instance();

        instance.handleMessage({ name: '', message: 'sent' });

        expect(lcEvents.execute).toHaveBeenCalledTimes(1);
        expect(lcEvents.execute).toHaveBeenCalledWith('onSubmit', []);

        expect(executeSubmitGoal).toHaveBeenCalledTimes(1);
        expect(executeSubmitGoal).toHaveBeenCalledWith({ section: 'YandexForm', from: 'Self' });
    });

    test('should provide params and source to url', () => {
        const wrapper = mount(
            <LcYandexFormComponent
                {...props}
                params={[{ name: 'manager', value: 'irina' }]}
                originalUrl={'https://yandex.ru/alice'}
            />
        );

        expect(wrapper.find('iframe').prop('src')).toBe(
            'https://forms.yandex.ru/surveys/yandex-form-id?manager=irina&lpc_url=https%3A%2F%2Fyandex.ru%2Falice&iframe=1&lang=ru'
        );
    });
});
