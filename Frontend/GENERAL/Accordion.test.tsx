import React from 'react';
import { render } from 'enzyme';

import { Accordion, AccordionHeader, AccordionContent, AccordionChevron } from './Accordion';

describe('Should render Accordion', () => {
    it('closed', () => {
        const wrapper = render(
            <Accordion>
                <AccordionHeader>
                    <AccordionChevron direction="bottom" />
                    Title
                </AccordionHeader>
                <AccordionContent>
                    Content
                </AccordionContent>
            </Accordion>
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('open', () => {
        const wrapper = render(
            <Accordion open>
                <AccordionHeader>
                    <AccordionChevron direction="top" />
                    Title
                </AccordionHeader>
                <AccordionContent>
                    Content
                </AccordionContent>
            </Accordion>
        );

        expect(wrapper).toMatchSnapshot();
    });

    it('with click handlers', () => {
        const onClick = jest.fn();

        const wrapper = render(
            <Accordion>
                <AccordionHeader onClick={onClick}>
                    <AccordionChevron direction="bottom" onClick={onClick} />
                    Title
                </AccordionHeader>
                <AccordionContent onClick={onClick}>
                    Content
                </AccordionContent>
            </Accordion>
        );

        expect(wrapper).toMatchSnapshot();
    });
});
