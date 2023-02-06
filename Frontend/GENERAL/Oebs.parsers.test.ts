import { parseOebsAgreements } from './Oebs.parsers';
import { getOebsAgreementMock, getRawOebsAgreementMock } from '~/test/jest/mocks/data/oebs-agreement';

describe('Oebs parsers', () => {
    it('should convert OebsAgreements to frontend format in parseOebsAgreements', () => {
        expect(parseOebsAgreements({
            next: 'next',
            previous: 'previous',
            results: [
                getRawOebsAgreementMock(1, { id: 1 }),
                getRawOebsAgreementMock(1, { id: 2 }),
            ],
        })).toStrictEqual({
            next: 'next',
            previous: 'previous',
            results: [
                getOebsAgreementMock(1, { id: 1 }),
                getOebsAgreementMock(1, { id: 2 }),
            ],
        });
    });

    it('should not fail on empty results-array in parseOebsAgreements', () => {
        expect(parseOebsAgreements({
            next: 'next',
            previous: 'previous',
            results: [],
        })).toStrictEqual({
            next: 'next',
            previous: 'previous',
            results: [],
        });
    });
});
