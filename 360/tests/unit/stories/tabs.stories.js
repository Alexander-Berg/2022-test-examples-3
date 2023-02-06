import { storiesOf, specs, describe, mockFunction, it, snapshot, mount } from '../.storybook/facade';

import React from 'react';
import Tabs from '../../../components/rocks/tabs';
import '../../../components/rocks/tabs/index.styl';
import { Link } from '@ps-int/ufo-rocks/lib/components/lego-components/Link';

export default storiesOf('Tabs', module)
    .add('default', ({ kind, story }) => {
        const items = [{
            id: 'allPhotos',
            link: '/client/photo',
            title: 'Все фото'
        }, {
            id: 'albums',
            link: '/client/albums',
            title: 'Альбомы'
        }];

        const onChange = mockFunction();

        const component = <Tabs
            items={items}
            current="albums"
            onChange={onChange}
        />;

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('calls onChange callback', () => {
                expect.assertions(1);
                const wrapper = mount(component);
                wrapper
                    .find(Link)
                    .findWhere((link) => link.prop('href') === '/client/photo')
                    .first()
                    .simulate('click');

                expect(onChange).toHaveBeenCalledWith('allPhotos');
            });
        }));

        return component;
    });
