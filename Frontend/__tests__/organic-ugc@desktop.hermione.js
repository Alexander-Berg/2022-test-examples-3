function loadPage(snippet, url) {
    return this.browser
        .url(url || '/turbo?data=H4sIAAAAAAAAE52PTQ6DIBCFT4M7DPhXu2ChtSQ9Bm1JJIJaxZr29EUUQ9NdN4+PgfdmRgstOQEVAgUGEVqgLKz6nFs9bbx9o6DCIF%2F5bDWxldgZDUdelG83T9gZDR8sp98Ww2dXWf8bpc6OvOTVXrpR98DKcrmFf%2FWlXsfM23Hf7ndN232L3SfH3sw5iGkwDZLUWvcjiAsQmXHpPM8hQseQKXNpuOJD9+wMsl7zRsBmel%2FZODLYD1DyVrQMYnQIa61kwFst9OtyJ3%2F61Y3gKF6OJM2Cfhprsgh8MjnxYOAPcSdW18oH+34EdA0CAAA%3D&brand=organic-ugc&checksum=Ndjv0Shlt9qC2UcJo4C2NZPpT+WY3PxYuZx2K8CpNPg%3D&text=bin%3Aorganic-ugc_type_comments_desktop-alpha-gz')
        .execute(snippet => {
            window.Ya = {
                ...(window.Ya || {}), ...{
                    Cmnt: {
                        api: {
                            fetchTree: () => Promise.resolve({ meta: {} }),
                            getRCA: () => Promise.resolve(snippet),
                        },
                    },
                },
            };
        }, snippet)
        .yaWaitForVisible(PO.SerpOrganicParagraphNotEmpty());
}

specs({
    feature: 'Organic ugc',
}, () => {
    describe('landing page', function() {
        it('Проверка внешнего вида', function() {
            const snippet = {
                snippet: 'Другие аптеки рядом. Аптека 36.6, пр. Октябрьский, 61-Б. Аптека Фармаимпекс, пр. Ленина, 105. Аптечный дом, пр. Ленина, 103. Аптечный дом, пр. Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...',
                image: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1048015\/2a0000016cf4dd8cf9003259ac32dac18f0d\/largePreview',
                url: 'https:\/\/www.009.am\/kemerovo\/apteki-kuzbassa-pr-lenina-107.html',
                title: 'Аптеки Кузбасса | Справочная аптек 009.рф',
                host: 'www.009.am',
            };

            return loadPage.call(this, snippet)
                .assertView('plain', PO.Organic());
        });

        it('Проверка внешнего вида при переполнении полей', function() {
            const url = '/turbo?data=H4sIAAAAAAAAE+1TyW6DMBD9GnMD2QYSc+BAWKR+Bm0sBQUSCiZR+/XF+6SlUq+VIkUvw3iW98Ye0Yme56jCKKskHlKFiUKskCrUpzWizkkQO9jPysavTqbsg0Lm47VRNCA+AR0LlUuMh+pSlULdK7ZOApwY8KxMR+nf6ZTIdsWAHwGdCpC/A/nU8iitgMbo95HY6lTkpJ098lMxvgLsVYA5OmeJ4gZhFZZRQJg9VqhNsMfU6gd9/XSY4eaF6KMVG1Ckfrwql66L74FMLbn80Q7a6dbc2MbkTa8YTIPZx6EZqsoU/3IvBfA7CbpRZivDCnsYH24lO8ZKxvdbT/0LramcUZbYxNpcw4bycquOo+U0l9/Xxe2ZtAkgZu4mCpapz09CjDOK10fVrL/7/R5hnEXtsH6c+cCn6+0qj55L/lzy55L/wyVfl7cdBT934Xn5fG3nuQ3HKez5pbu0IcH76CSGPuAX0YmPl2O+tfx/yB/eckJj+Zeku2Bc5lMuIby1/cKDib93x1yh9nwBAPwX2DoIAAA=&checksum=GGyNHwW69QOCBG/FpThW8p4KWLp5eog+T3qLNVM5tX4=&brand=organic-ugc&text=bin:organic-ugc_type_comments_desktop-alpha-gz';
            const snippet = {
                snippet: 'Другие аптеки рядом. Аптека 36.6, пр. Октябрьский, 61-Б. Аптека Фармаимпекс, пр. Ленина, 105. Аптечный дом, пр. Ленина, 103. Аптечный дом, пр. Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...',
                image: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1048015\/2a0000016cf4dd8cf9003259ac32dac18f0d\/largePreview',
                url: 'https:\/\/www.009.am\/kemerovo\/apteki-kuzbassa-pr-lenina-107.html',
                title: 'Аптеки Кузбасса | Справочная аптек 009.рф',
                host: 'www.009.am',
            };

            return loadPage.call(this, snippet, url)
                .assertView('plain', PO.Organic());
        });

        it('Проверка внешнего вида с эмодзи в тексте', function() {
            const snippet = {
                snippet: '😀 😬 😁 😂 😃Другие аптеки рядом. Аптека 36.6, пр. Октябрьский, 61-Б. Аптека Фармаимпекс, пр. Ленина, 105. Аптечный дом, пр. Ленина, 103. Аптечный дом, пр. Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...',
                image: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1048015\/2a0000016cf4dd8cf9003259ac32dac18f0d\/largePreview',
                url: 'https:\/\/www.009.am\/kemerovo\/apteki-kuzbassa-pr-lenina-107.html',
                title: 'Аптеки Кузбасса | Справочная аптек 009.рф',
                host: 'www.009.am',
            };

            return loadPage.call(this, snippet)
                .assertView('plain', PO.Organic());
        });

        it('Проверка внешнего вида с длинным словом в тексте', function() {
            const snippet = {
                snippet: 'Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...',
                image: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1048015\/2a0000016cf4dd8cf9003259ac32dac18f0d\/largePreview',
                url: 'https:\/\/www.009.am\/kemerovo\/apteki-kuzbassa-pr-lenina-107.html',
                title: 'Аптеки Кузбасса | Справочная аптек 009.рф',
                host: 'www.009.am',
            };

            return loadPage.call(this, snippet)
                .assertView('plain', PO.Organic());
        });

        it('Проверка внешнего вида c очень длинным текстом', function() {
            const snippet = {
                snippet: 'Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, прДругиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...Другиеаптекирядом.Аптека36.6,пр.Октябрьский,61-Б.Аптека Фармаимпекс,пр. Ленина,105.Аптечныйдом,пр.Ленина,103.Аптечныйдом,пр.Ленина, 117. Аптечный дом, ул. Терешковой, 22-А. Мир лекарств, пр. Ленина, 103. Фармацевт АП №6, ул. Гагарина, 151. ФармЦент, пр...',
                image: 'https:\/\/avatars.mds.yandex.net\/get-shinyserp\/1048015\/2a0000016cf4dd8cf9003259ac32dac18f0d\/largePreview',
                url: 'https:\/\/www.009.am\/kemerovo\/apteki-kuzbassa-pr-lenina-107.html',
                title: 'Аптеки Кузбасса | Справочная аптек 009.рф',
                host: 'www.009.am',
            };

            return loadPage.call(this, snippet)
                .assertView('plain', PO.Organic());
        });
    });
});
