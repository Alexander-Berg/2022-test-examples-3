import { updateContentHeader, IContentHeaderUpdate } from './updateContentHeader';

import { ILcRootTurboJSON } from '../../typings/lpc';

import { getContentHeaderSection } from '../stubs/lpcContentHeaderSection';

describe('updateContentHeader', () => {
    let params: IContentHeaderUpdate;

    let contentHeaderSection: ILcRootTurboJSON;

    beforeEach(() => {
        params = {
            itemDescription: {
                id: 123,
                icon: 'icon',
                description: 'lorem ipsum',
                name: 'name',
            },
        };

        contentHeaderSection = getContentHeaderSection();
    });

    it('should update header via itemDescription name', () => {
        const { header } = updateContentHeader(params).updates;

        const result = header(contentHeaderSection)?.content;

        expect(result).toBeTruthy();
        expect(result).toBe(params.itemDescription.name);
    });

    it('should update content via itemDescription description', () => {
        const { content } = updateContentHeader(params).updates;

        const result = content(contentHeaderSection)?.content;

        expect(result).toBeTruthy();
        expect(result).toBe(params.itemDescription.description);
    });

    it('should update logo url via itemDescription icon', () => {
        const { logo } = updateContentHeader(params).updates;

        const result = logo({ ...contentHeaderSection, image: {
            url: 'old url',
        } })?.image;

        expect(result).toBeTruthy();
        expect(result).toHaveProperty('url', params.itemDescription.icon);
    });

    it('should preserve logo if it has not image field', () => {
        const sectionContentCopy = getContentHeaderSection().content;

        const { logo } = updateContentHeader(params).updates;

        const result = logo(contentHeaderSection)?.content;

        expect(result).toBeTruthy();
        expect(result).toStrictEqual(sectionContentCopy);
    });
});
