import { needUserActivityInfoForPromoBanner } from '../../../components/helpers/promo';

describe('promoHelper -->', () => {
    describe('needUserActivityInfoForPromoBanner', () => {
        const getData = ({
            experiment = {},
            justInitialized = false,
            country_tld = 'ru',
            isMobile = false,
            OSFamily = 'Windows'
        } = {}) => ({
            experiment,
            user: { justInitialized },
            region: { country_tld },
            agent: { isMobile, OSFamily }
        });

        it('Возвращает true для параметров, заданных по умолчанию', () => {
            expect(needUserActivityInfoForPromoBanner(getData(), 'desktop-soft')).toBe(true);
        });

        it('Возвращает false, если пользователь зашел в мобильный клиент или страна пользователя - не Россия, или это первый заход в веб-клиент', () => {
            expect(needUserActivityInfoForPromoBanner(getData({ isMobile: true }), 'desktop-soft')).toBe(false);
            expect(needUserActivityInfoForPromoBanner(getData({ isMobile: false }), 'desktop-soft')).toBe(true);

            expect(needUserActivityInfoForPromoBanner(getData({ country_tld: 'en' }), 'desktop-soft')).toBe(false);
            expect(needUserActivityInfoForPromoBanner(getData({ country_tld: 'ru' }), 'desktop-soft')).toBe(true);

            expect(needUserActivityInfoForPromoBanner(getData({ justInitialized: true }), 'desktop-soft')).toBe(false);
            expect(needUserActivityInfoForPromoBanner(getData({ justInitialized: false }), 'desktop-soft')).toBe(true);
        });

        it('Возвращает true, если платформа - Windows или MacOS', () => {
            expect(needUserActivityInfoForPromoBanner(getData({
                OSFamily: 'Windows'
            }), 'desktop-soft')).toBe(true);
            expect(needUserActivityInfoForPromoBanner(getData({
                OSFamily: 'MacOS'
            }), 'desktop-soft')).toBe(true);
            expect(needUserActivityInfoForPromoBanner(getData({
                experiment: { diskWebClientMobileTest: true },
                OSFamily: 'Windows'
            }), 'desktop-soft')).toBe(true);
            expect(needUserActivityInfoForPromoBanner(getData({
                OSFamily: 'Linux'
            }), 'desktop-soft')).toBe(false);
        });

        it('Возвращает true, если пользователь попал в эксперимент на промо-баннер для скачивания мобильного приложения', () => {
            expect(needUserActivityInfoForPromoBanner(getData({
                experiment: { diskWebClientMobileTest: true }
            }), 'mobile-app')).toBe(true);
            expect(needUserActivityInfoForPromoBanner(getData(), 'mobile-app')).toBe(false);
        });
    });
});
