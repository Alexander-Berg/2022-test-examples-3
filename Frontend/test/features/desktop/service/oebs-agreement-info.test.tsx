import { render } from '~/test/features/desktop/service/oebs-agreement-info.po';
import { OebsAgreementTerminatedStates } from '~/src/features/Oebs/Oebs.types';

describe('OebsAgreementInfo', () => {
    ['approving', 'approved'].forEach(
        state => it(`Should show the message with link for state ${state}`, () => {
            const oebsAgreementInfo = render({ state: state, issue: 'TESTABC-123' });

            expect(oebsAgreementInfo.infoMessage?.container).toBeInTheDocument();
            expect(oebsAgreementInfo.link).toBe('TESTABC-123');
            expect(oebsAgreementInfo.url).toBe('https://tracker.mock/TESTABC-123');
        }),
    );

    ['open', 'validating_in_oebs', 'validated_in_oebs', 'applying_in_oebs', 'applied_in_oebs'].forEach(
        state => it(`Should show only message without link if state is ${state}`, () => {
            const oebsAgreementInfo = render({ state: state, issue: 'TESTABC-123' });

            expect(oebsAgreementInfo.infoMessage?.container).toBeInTheDocument();
            expect(oebsAgreementInfo.infoMessage?.container.getElementsByClassName('.Link').length).toBe(0);
        }),
    );

    OebsAgreementTerminatedStates.forEach(
        state => it(`Should not show if state is ${state}`, () => {
            const oebsAgreementInfo = render({ state: state, issue: 'TESTABC-123' });

            expect(oebsAgreementInfo.container).toBeEmptyDOMElement();
        }),
    );
});
