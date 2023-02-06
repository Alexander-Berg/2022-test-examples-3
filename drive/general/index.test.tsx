import React from 'react';
import { render } from '@testing-library/react';

import DashboardScoringLabel from 'components/ui/DashboardScoringLabel/index';

describe('ScoringLabel test', () => {
    describe('ScoringLabel title', () => {
        const MOCK_SCORE = 3;

        it('Should match snapshot with default title', () => {
            let { baseElement } = render(<DashboardScoringLabel score={MOCK_SCORE} />);
            expect(baseElement).toMatchSnapshot();
        });

        it('Should match snapshot with custom title', () => {
            let { baseElement } = render(
                <DashboardScoringLabel
                    title="custom title"
                    score={MOCK_SCORE}
                />,
            );
            expect(baseElement).toMatchSnapshot();
        });
    });

    describe('ScoringLabel score', () => {
        it('Should match snapshot with zero score', () => {
            let { baseElement } = render(<DashboardScoringLabel score={0} />);
            expect(baseElement).toMatchSnapshot();
        });

        it('Should match snapshot with bad score', () => {
            let { baseElement } = render(<DashboardScoringLabel score={1} />);
            expect(baseElement).toMatchSnapshot();
        });

        it('Should match snapshot with normal score', () => {
            let { baseElement } = render(<DashboardScoringLabel score={3.5} />);
            expect(baseElement).toMatchSnapshot();
        });

        it('Should match snapshot with good score', () => {
            let { baseElement } = render(<DashboardScoringLabel score={4.5} />);
            expect(baseElement).toMatchSnapshot();
        });

        it('Should match snapshot with three-digits score', () => {
            let { baseElement } = render(<DashboardScoringLabel score={4.123} />);
            expect(baseElement).toMatchSnapshot();
        });
    });
});
