
import { storiesOf, specs, snapshot, mount } from '../.storybook/facade';
import React from 'react';

import PageStub from '../../../components/rocks/page-stub';
import '../../../components/rocks/page-stub/index.styl';
import '../../../components/redux/components/listing/listing__stub.styl';

import bem from 'easy-bem-naming';
const b = () => bem()('listing-stub');

export default storiesOf('PageStub', module)
    .add('listing', ({ kind, story }) => {
        const component = <PageStub>
            <span className={b().e('background')} />
            <section className={b().e('desc')}>
                <h1>Content Caption</h1>
            </section>
        </PageStub>;

        specs(() => describe(kind, () => {
            snapshot(story, component);

            it('unmounts properly immediately', (done) => {
                expect.assertions(1);
                const wrapper = mount(component);
                setTimeout(() => {
                    expect(wrapper.state().animated).toBe(false);
                    wrapper.unmount();
                    done();
                });
            });

            it('unmounts properly after second', (done) => {
                expect.assertions(1);
                const wrapper = mount(component);
                setTimeout(() => {
                    expect(wrapper.state().animated).toBe(true);
                    wrapper.unmount();
                    done();
                }, 1000);
            });
        }));

        return component;
    });
