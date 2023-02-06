/* eslint-disable */
import { removeAsterisks, stripProtocol, checkIfSubdomain } from './csp';

describe('removeAsterisks', () => {
    it('should remove subdomain asterisk', () => {
        expect(removeAsterisks('*.example.com')).toBe('example.com');
        expect(removeAsterisks('https://*.example.com')).toBe('https://example.com');
    });

    it('should remove port number asterisk', () => {
        expect(removeAsterisks('example.com:*')).toBe('example.com');
        expect(removeAsterisks('https://example.com:*')).toBe('https://example.com');
    });

    it('should remove port number asterisk', () => {
        expect(removeAsterisks('example.com:*')).toBe('example.com');
        expect(removeAsterisks('https://example.com:*')).toBe('https://example.com');
    });

    it('should remove both asterisks', () => {
        expect(removeAsterisks('*.example.com:*')).toBe('example.com');
        expect(removeAsterisks('https://*.example.com:*')).toBe('https://example.com');
    });

    it('should not remove ', () => {
        expect(removeAsterisks('examp*le.com')).toBe('examp*le.com');
        expect(removeAsterisks('https://examp*le.com')).toBe('https://examp*le.com');
    });
});

describe('removeProtocol', () => {
    it('sould remove protocol', () => {
        expect(stripProtocol('https://example.com')).toEqual(['example.com', 'https:']);
    });

    it('sould remove protocol', () => {
        expect(stripProtocol('http://example.com')).toEqual(['example.com', 'http:']);
    });

    it('sould remove protocol', () => {
        expect(stripProtocol('//example.com')).toEqual(['example.com', '']);
    });
});

describe('checkIfSubdomain', () => {
    it('should check valid subdomain 1', () => {
        expect(checkIfSubdomain('subdomain.example.com', 'example.com')).toBe(true);
    });

    it('should check valid subdomain 2', () => {
        expect(checkIfSubdomain('subdomain.subdomain.example.com', 'example.com')).toBe(true);
    });

    it('should check valid subdomain 2', () => {
        expect(checkIfSubdomain('example.com', 'example.com')).toBe(true);
    });

    it('should check invalid subdomain 1', () => {
        expect(checkIfSubdomain('evil-example.com', 'example.com')).toBe(false);
    });

    it('should check invalid subdomain 2', () => {
        expect(checkIfSubdomain('example.com.evil', 'example.com')).toBe(false);
    });
});
