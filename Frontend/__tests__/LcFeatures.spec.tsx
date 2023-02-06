import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { LcEventType, LcSectionType } from '@yandex-turbo/components/LcEvents/LcEvents.constants';
import presets from '../LcFeatures.presets';
import { LcFeaturesComponent as LcFeatures, ILcFeaturesProps, ILcFeaturesItemProps } from '../LcFeatures';

describe('<LcFeatures/> component', () => {
    let wrapper: ReactWrapper;

    afterEach(() => {
        wrapper.unmount();
    });

    test('should pass events to link', () => {
        const items = presets.default.items.map(item => ({
            ...item,
            featureAction: ({
                ...item.featureAction,
                buttonLabel: 'Кнопка',
                linkText: 'Ссылка',
                disclaimer: 'Дисклеймер',
            } as unknown) as ILcFeaturesItemProps['featureAction'],
            link: {
                ...item.link,
                events: [
                    {
                        type: LcEventType.OnClick,
                        target: { type: LcSectionType.LcAnalytics },
                        data: {},
                    },
                ],
            },
        }));

        const props: ILcFeaturesProps = {
            ...presets.default,
            items,
            columns: '3',
            sectionId: '',
        };

        wrapper = mount(<LcFeatures {...props} />);

        const links = wrapper.find('.lc-features__area-link').filterWhere(node => {
            const type = node.type();

            // @ts-ignore
            return type && type.displayName === 'LcLink';
        });

        expect(links.length).toBe(3);

        links.forEach(node => {
            expect(node.prop('events')).toEqual([{
                type: LcEventType.OnClick,
                target: { type: LcSectionType.LcAnalytics },
                data: {},
            }]);
        });
    });
});
