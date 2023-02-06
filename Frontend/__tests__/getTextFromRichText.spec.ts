import { TRichContent, IRichText } from '@yandex-turbo/components/LcRichText/LcRichText';
import { getTextFromRichText } from '../LcPhone.helpers';

describe('getTextFromRichText', () => {
    let content: TRichContent;

    beforeEach(() => {
        content = {
            content: {
                content: 'content text',
            },
        };
    });

    it('should handle nested richText', () => {
        expect(getTextFromRichText(content)).toBe('content text');
    });

    it('should handle nested richText with tags', () => {
        ((content as IRichText).content as IRichText).content = '<p><b>content text 123</b></p>';

        expect(getTextFromRichText(content)).toBe('content text 123');
    });

    it('should handle string in richText', () => {
        content = 'content 456';
        expect(getTextFromRichText(content)).toBe('content 456');

        content = '<b>content 135</b>';
        expect(getTextFromRichText(content)).toBe('content 135');
    });

    it('should handle tags with attributes', () => {
        content = '<p><b style="color: #000">content12345</b></p>';

        expect(getTextFromRichText(content)).toBe('content12345');
    });

    it('should handle array with mixed richText', () => {
        content = [
            {
                content: {
                    content: '<b>richtext text content</b>',
                },
            },
            '<b>second content</b>',
            [
                'third content',
                {
                    content: '<p style="color: #000">fourth content</p>',
                },
            ],
        ] as TRichContent;

        expect(getTextFromRichText(content)).toBe('richtext text content second content third content fourth content');
    });
});
