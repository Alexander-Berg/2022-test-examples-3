import * as React from 'react';
import { shallow } from 'enzyme';
import { EVideoSettings } from '@yandex-turbo/core/state/news-video-settings/reducer';
import { NewsVideoSettingsPresenter as NewsVideoSettings } from '../NewsVideoSettings';

describe('NewsVideoSettings component', () => {
    const props = {
        items: [
            { name: EVideoSettings.AUTOPLAY, text: 'Автоплей видео' },
        ],
    };

    it('должен рендерится без ошибок', () => {
        const wrapper = shallow(<NewsVideoSettings {...props} />);

        expect(wrapper.length).toEqual(1);
    });

    it('при изменении настройки "Автоплей видео" должна вызваться функция setCookie', () => {
        // <any> для доступа к приватному методу setCookie экземляра класса NewsVideoSettings
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const setCookieSpy = spyOn<any>(NewsVideoSettings.prototype, 'setCookie');
        const wrapper = shallow(<NewsVideoSettings {...props} />);
        const mockEvent = {
            target: { name: EVideoSettings.AUTOPLAY },
            checked: true,
        };

        wrapper.setState({ visible: true });
        wrapper.find(`[name="${EVideoSettings.AUTOPLAY}"]`).simulate('change', mockEvent);
        expect(setCookieSpy).toHaveBeenCalledTimes(1);
    });
});
