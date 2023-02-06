import React from 'react';
import Controls from './Controls';
import { shallow } from 'enzyme';
import toJSON from 'enzyme-to-json';

const CONTROLS = ['more', 'reply', 'reply-all', 'trash', 'compose'];

describe('<Controls>', () => {
    it('works without props', () => {
        const component = shallow(<Controls />).dive();
        expect(toJSON(component)).toMatchSnapshot();
    });

    CONTROLS.forEach((control) => {
        it(`works with ${control} control`, () => {
            const component = shallow(<Controls controls={[control]} />).dive();
            expect(toJSON(component)).toMatchSnapshot();
        });
    });

    it('works with all controls', () => {
        const component = shallow(<Controls controls={CONTROLS} />).dive();
        expect(toJSON(component)).toMatchSnapshot();
    });

    it('works with non-existing control', () => {
        const component = shallow(<Controls controls={['foo', 'bar', 'more', 'reply']} />).dive();
        expect(toJSON(component)).toMatchSnapshot();
    });

    it('reply + reply-all = reply-all', () => {
        const component = shallow(<Controls controls={['reply', 'reply-all']} />).dive();
        expect(toJSON(component)).toMatchSnapshot();
    });

    it('reply-all + reply = reply', () => {
        const component = shallow(<Controls controls={['reply-all', 'reply']} />).dive();
        expect(toJSON(component)).toMatchSnapshot();
    });
});
