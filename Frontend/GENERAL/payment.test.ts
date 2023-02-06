import { getCardLabel } from './payment';

describe('Helpers: getCardLabel', () => {
    it('should return empty string if name and number is empty', () => {
        expect(getCardLabel({ name: '', number: '' })).toEqual('');
        expect(getCardLabel({ name: '' })).toEqual('');
        expect(getCardLabel({ number: '' })).toEqual('');
        expect(getCardLabel({})).toEqual('');
    });

    it('should return only card name if number is empty', () => {
        expect(getCardLabel({ name: 'MasterCard', number: '' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'Visa' })).toEqual('Visa');
    });

    it('should return only card name if number is invalid', () => {
        expect(getCardLabel({ name: 'MasterCard', number: 'inv@l!d' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'Visa', number: '****' })).toEqual('Visa');
        expect(getCardLabel({ name: 'Мир', number: '1234****' })).toEqual('Мир');
        expect(getCardLabel({ name: 'MasterCard', number: '****1234****' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'MasterCard', number: '123456789012' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'MasterCard', number: '**** 1234' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'MasterCard', number: 'a****1234' })).toEqual('MasterCard');
        expect(getCardLabel({ name: 'MasterCard', number: '****1234a' })).toEqual('MasterCard');
    });

    it('should format number', () => {
        expect(getCardLabel({ name: 'MasterCard', number: '****1234' })).toEqual('MasterCard •••• 1234');
        expect(getCardLabel({ name: 'Visa', number: '1234****5678' })).toEqual('Visa •••• 5678');
        expect(getCardLabel({ name: 'Мир', number: '1****1234' })).toEqual('Мир •••• 1234');
        expect(getCardLabel({ name: 'MasterCard', number: '1*1234' })).toEqual('MasterCard •••• 1234');
        expect(getCardLabel({ name: 'MasterCard', number: '****1' })).toEqual('MasterCard •••• 1');
    });

    it('should return only formated number if name is empty', () => {
        expect(getCardLabel({ number: '****1234' })).toEqual('•••• 1234');
        expect(getCardLabel({ name: '', number: '****5678' })).toEqual('•••• 5678');
    });
});
