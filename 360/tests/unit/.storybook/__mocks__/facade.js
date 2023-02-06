import { render, serialize, renderIntoDocument } from '../../snapshots-options';
import 'dom-testing-library/extend-expect';

export const storiesOf = (kind) => ({
    add (story, func) {
        func({ kind, story });
        return this;
    },
    addWithInfo (story, func) {
        func({ kind, story });
        return this;
    }
});

export const specs = (spec) => spec();

export const snapshot = (name, story) => {
    it(name, function () {
        const tree = render(story);
        expect(serialize(tree)).toMatchSnapshot();
        tree.unmount();
    });
};

export const describe = jasmine.currentEnv_.describe;
export const it = jasmine.currentEnv_.it;
export const itJest = it;
export const mockFunction = jest.fn;

export { render, serialize, renderIntoDocument };
export { mount, shallow, render as renderString } from 'enzyme';
export { fireEvent, wait, Simulate } from 'react-testing-library';
