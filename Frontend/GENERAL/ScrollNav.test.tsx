import React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { ElementAnchor } from './ScrollNav.lib';
import { ScrollNav } from './ScrollNav';

let component: ReactWrapper;

const anchors: ElementAnchor[] = ['1', '2', '3'].map(
    anchor => ({
        href: anchor,
        content: anchor,
        ref: React.createRef(),
    }),
);
const tautology = () => true;
const contradiction = () => false;

describe('ScrollNav', () => {
    afterEach((): void => {
        component.unmount();
    });

    it('should render unchecked links', () => {
        component = mount(
            <ScrollNav
                anchors={anchors}
                predicate={contradiction}
            />
        );

        const links = component.find('a.Link.Link_view_button');
        expect(links.length).toEqual(3);
    });

    it('should render checked links', () => {
        component = mount(
            <ScrollNav
                anchors={anchors}
                predicate={tautology}
            />
        );

        const links = component.find('a.Link.Link_view_button-checked');
        expect(links.length).toEqual(3);
    });
});
