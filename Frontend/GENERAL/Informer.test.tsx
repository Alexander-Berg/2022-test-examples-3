import * as React from 'react';
import { mount } from 'enzyme';

import { InformerDesktop } from './Informer@desktop';
import { InformerTouch } from './Informer@touch';

const platforms = [
    ['desktop', InformerDesktop],
    ['touch', InformerTouch],
];

describe.each(platforms)('Informer@%s', (_platform, Informer) => {
    it('Should render with none props', () => {
        const wrapper = mount(
            <Informer
                lang="ru"
                data={{
                    currentInformerTimeout: 1,
                    id: 'coronavirus',
                    title: {
                        ru: 'Вторая волна',
                        en: 'The second wave',
                    },
                    iconUrl: 'https://jing.yandex-team.ru/files/tet4enko/pngwing.com.png',
                    text: {
                        ru: 'Давайте посидим дома',
                        en: 'Let\'s stay at home',
                    },
                    button: {
                        text: {
                            ru: 'Посмотреть статистику (latest)',
                            en: 'See stats',
                        },
                        link: 'https://coronavirus-monitor.ru/',
                    },
                }}
            />,
        );

        expect(wrapper).toMatchSnapshot();
        wrapper.unmount();
    });
});
