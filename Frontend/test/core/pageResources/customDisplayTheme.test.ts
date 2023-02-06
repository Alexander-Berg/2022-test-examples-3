import { CustomDisplayThemeStyles } from '../../../src/core/pageResources/resources/styles/customDisplayTheme';
import { IFonts } from '../../../src/core/pageResources/resources/styles/utils/customDisplayThemeTypes';

interface IApplyFontsData {
    props: IFonts;
    result: string;
}

const applyFontsData: IApplyFontsData[] = [{
    props: {
        headers: {
            type: 'sans-serif',
            size: 'small'
        },
        text: {
            type: 'sans-serif',
            size: 'small'
        }
    },
    result: '',
}, {
    props: {
        headers: {
            type: 'serif',
            size: 'small'
        },
        text: {
            type: 'serif',
            size: 'small'
        }
    },
    result: 'custom-theme-fonts-title_type_serif custom-theme-fonts-title_size_small-type-serif custom-theme-fonts-text_type_serif custom-theme-fonts-text_size_small-type-serif',
}, {
    props: {
        headers: {
            type: 'sans-serif',
            size: 'medium'
        },
        text: {
            type: 'sans-serif',
            size: 'medium'
        }
    },
    result: 'custom-theme-fonts-title_size_medium-type-sans custom-theme-fonts-text_size_medium-type-sans',
}, {
    props: {
        headers: {
            type: 'serif',
            size: 'medium'
        },
        text: {
            type: 'serif',
            size: 'medium'
        }
    },
    result: 'custom-theme-fonts-title_type_serif custom-theme-fonts-title_size_medium-type-serif custom-theme-fonts-text_type_serif custom-theme-fonts-text_size_medium-type-serif',
}, {
    props: {
        headers: {
            type: 'sans-serif',
            size: 'large'
        },
        text: {
            type: 'sans-serif',
            size: 'large'
        }
    },
    result: 'custom-theme-fonts-title_size_large-type-sans custom-theme-fonts-text_size_large-type-sans',
}, {
    props: {
        headers: {
            type: 'serif',
            size: 'large'
        },
        text: {
            type: 'serif',
            size: 'large'
        }
    },
    result: 'custom-theme-fonts-title_type_serif custom-theme-fonts-title_size_large-type-serif custom-theme-fonts-text_type_serif custom-theme-fonts-text_size_large-type-serif',
}];

describe('CustomDisplayThemeStyles', () => {
    applyFontsData.forEach((el, i) => {
        it(`Пушатся правильные названия бандлов для размеров и стилей шрифтов ${i}`, () => {
            const CustomDisplayTheme = new CustomDisplayThemeStyles();
            CustomDisplayTheme.bundlesStyles = [];
            CustomDisplayTheme.applyFonts(el.props);
            const bundlesNames = CustomDisplayTheme.bundlesStyles.join(' ');

            expect(el.result).toEqual(bundlesNames);
        });
    });
});
