import { getCarOfferStatus } from 'entities/Car/helpers/getCarOfferStatus/getCarOfferStatus';
import { CarOfferSchema } from 'entities/Car/types/CarOfferSchema';

const STATUSES = [
    { title: 'Draft', id: 'draft' },
    { title: 'Confirmed', id: 'confirmed' },
    { title: 'Paid', id: 'paid' },
];

describe('getCarOfferStatus', function () {
    it('should works', function () {
        expect(getCarOfferStatus('draft', STATUSES)).toMatchInlineSnapshot(`"Draft"`);
        expect(getCarOfferStatus('confirmed', STATUSES)).toMatchInlineSnapshot(`"Confirmed"`);
        expect(getCarOfferStatus('paid', STATUSES)).toMatchInlineSnapshot(`"Paid"`);
        expect(getCarOfferStatus('unknown' as CarOfferSchema['status'], STATUSES)).toMatchInlineSnapshot(`"???"`);
    });
});
