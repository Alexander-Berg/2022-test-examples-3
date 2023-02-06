hermione.only.in(['chrome-phone', 'iphone', 'searchapp']);
specs({
    feature: 'Organic ugc',
}, () => {
    describe('landing page', function() {
        hermione.only.notIn('safari13');
        it('Проверка внешнего вида', function() {
            return this.browser
                .url('/turbo?text=organic-ugc_type_comments_alpha&checksum=lsat21TsyyyJLdBGKD8NQ28jjiGl+0jei+VImMR6xeo%3D&brand=organic-ugc&data=dGl0bGU9JUQwJTkyJUQwJUI4JUQwJUJBJUQwJUI4JUQwJUJGJUQwJUI1JUQwJUI0JUQwJUI4JUQxJThGJTIwJUQwJUFGJUQwJUJEJUQwJUI0JUQwJUI1JUQwJUJBJUQxJTgxJUQwJUIwJnVybD1odHRwcyUzQSUyRiUyRndpa2kueWFuZGV4LXRlYW0ucnUlMkZydHglMkYmZW50aXR5SWQ9d2lraS55YW5kZXgtdGVhbS5ydSUyRnJ0eCUyRg%3D%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида с gzipped данными', function() {
            return this.browser
                .url('/turbo?data=H4sIAAAAAAAAE22P0Q6CIBSGn4bubAhieeFFpm49BtWZMQ0coK6ePjzm5lo3P985fOcwvPId5KSkJEvmLApSxuRYE0axLDEpNg8rLxryrxbyvGmGKY6cbrRqFWIUis3C6t%2FCDDWGfFyFM6ZYr0LSrzDnaXl6N9guf3jfO8JPhIVf1dM07XszSgvatHs7hJaFm+rBBXIPM4UjFlnMWYAdaK%2F863LPKXvL1uhloDFucN3QqOgtn6Bl1Fs5RndzlWMHWkHUSg+NsUrNO563XDCeCJ7MeEhFmn4AOjeDQnYBAAA%3D&checksum=ahF5ccvi9EGMiePg+ZUhkOycs4h5wXvG3TuINLIC7z0%3D&brand=organic-ugc&text=bin%3Aorganic-ugc_type_comments-gz_alpha')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида при переполнении полей', function() {
            return this.browser
                .url('/turbo?checksum=43ZWJdvmN9BO7mgJOvoW3lKS5QpGT242fHBSgs0CWgg%3D&text=organic-ugc_type_comments_alpha&brand=organic-ugc&data=dGl0bGU9T25jZSUyMHVwb24lMjBhJTIwdGltZSUyMHRoZXJlJTIwbGl2ZWQlMjBpbiUyMGElMjBjZXJ0YWluJTIwdmlsbGFnZSUyMGElMjBsaXR0bGUlMjBjb3VudHJ5JTIwZ2lybCUyQyUyMHRoZSUyMHByZXR0aWVzdCUyMGNyZWF0dXJlJTIwd2hvJTIwd2FzJTIwZXZlciUyMHNlZW4uJTIwSGVyJTIwbW90aGVyJTIwd2FzJTIwZXhjZXNzaXZlbHklMjBmb25kJTIwb2YlMjBoZXIlM0IlMjBhbmQlMjBoZXIlMjBncmFuZG1vdGhlciUyMGRvdGVkJTIwb24lMjBoZXIlMjBzdGlsbCUyMG1vcmUuJTIwVGhpcyUyMGdvb2QlMjB3b21hbiUyMGhhZCUyMGElMjBsaXR0bGUlMjByZWQlMjByaWRpbmclMjBob29kJTIwbWFkZSUyMGZvciUyMGhlci4lMjBJdCUyMHN1aXRlZCUyMHRoZSUyMGdpcmwlMjBzbyUyMGV4dHJlbWVseSUyMHdlbGwlMjB0aGF0JTIwZXZlcnlib2R5JTIwY2FsbGVkJTIwaGVyJTIwTGl0dGxlJTIwUmVkJTIwUmlkaW5nJTIwSG9vZCZ1cmw9aHR0cHMlM0ElMkYlMkZ3d3cucGl0dHBpdHRwaXR0cGl0dHBpdHRwaXR0cGl0dHBpdHQuZWR1ZWR1ZWR1ZWR1ZWR1ZWR1ZWR1ZWR1JTJGfmRhc2hkYXNoZGFzaGRhc2hkYXNoZGFzaCUyRnR5cGUwMzMzdHlwZTAzMzN0eXBlMDMzM3R5cGUwMzMzdHlwZTAzMzN0eXBlMDMzM3R5cGUwMzMzdHlwZTAzMzMuaHRtbGh0bWxodG1saHRtbGh0bWxodG1saHRtbGh0bWxodG1saHRtbCUyM3BlcnJhdWx0cGVycmF1bHRwZXJyYXVsdHBlcnJhdWx0cGVycmF1bHRwZXJyYXVsdHBlcnJhdWx0cGVycmF1bHQmZW50aXR5SWQ9d2lraS55YW5kZXgtdGVhbS5ydSUyRnJ0eCUyRg%3D%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
        hermione.only.notIn('safari13');
        it('Проверка внешнего вида в горизонтальной ориентации', function() {
            return this.browser
                .setOrientation('landscape')
                .url('/turbo?text=organic-ugc_type_comments_alpha&checksum=lsat21TsyyyJLdBGKD8NQ28jjiGl+0jei+VImMR6xeo%3D&brand=organic-ugc&data=dGl0bGU9JUQwJTkyJUQwJUI4JUQwJUJBJUQwJUI4JUQwJUJGJUQwJUI1JUQwJUI0JUQwJUI4JUQxJThGJTIwJUQwJUFGJUQwJUJEJUQwJUI0JUQwJUI1JUQwJUJBJUQxJTgxJUQwJUIwJnVybD1odHRwcyUzQSUyRiUyRndpa2kueWFuZGV4LXRlYW0ucnUlMkZydHglMkYmZW50aXR5SWQ9d2lraS55YW5kZXgtdGVhbS5ydSUyRnJ0eCUyRg%3D%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида со спец.символами в тексте', function() {
            return this.browser
                .url('/turbo?brand=organic-ugc&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2B1YokIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOrl3OTRfSKSPjVw3t%2FmvTCDSg9nMwcWgZaMYENZF%2FahyNjZUZJgASUE+797CpMPnzQahvotZ2tnHuRfvgIiqeT4Uva6R++SFAC0oE76LURYt3hQbZ6hMprI7tkbCtGaM5ovuK5YEXxBTyF23yEAQAA&text=bin%3Abeauty_turbo_comments_symbols&checksum=ptXaYE3YdQFw1DAFY2mDRiBz+rxwGU7ihVcV8tty1Wc%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида с эмодзи в тексте', function() {
            return this.browser
                .url('/turbo?brand=organic-ugc&text=bin%3Abeauty_turbo_comments_emoji&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2B1YokIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOrl3OTRfSKSPjVw3t%2FmvTCDSg9nMwcWgZaMYENZF%2FahyNjZUZJgASUE+797CpMPnzQahvotZ2tnHuRfvgIiqeT4Uva6R++SFAC0oE76LURYt3hQbZ6hMprI7tkbCtGaM5ovuK5YEXxBTyF23yEAQAA&checksum=ptXaYE3YdQFw1DAFY2mDRiBz+rxwGU7ihVcV8tty1Wc%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида с длинным словом в тексте', function() {
            return this.browser
                .url('/turbo?text=bin%3Abeauty_turbo_comments_long_word&brand=organic-ugc&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2B1YokIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOrl3OTRfSKSPjVw3t%2FmvTCDSg9nMwcWgZaMYENZF%2FahyNjZUZJgASUE+797CpMPnzQahvotZ2tnHuRfvgIiqeT4Uva6R++SFAC0oE76LURYt3hQbZ6hMprI7tkbCtGaM5ovuK5YEXxBTyF23yEAQAA&checksum=ptXaYE3YdQFw1DAFY2mDRiBz+rxwGU7ihVcV8tty1Wc%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида с переносом текста на абзацы', function() {
            return this.browser
                .url('/turbo?brand=organic-ugc&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2B1YokIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOrl3OTRfSKSPjVw3t%2FmvTCDSg9nMwcWgZaMYENZF%2FahyNjZUZJgASUE+797CpMPnzQahvotZ2tnHuRfvgIiqeT4Uva6R++SFAC0oE76LURYt3hQbZ6hMprI7tkbCtGaM5ovuK5YEXxBTyF23yEAQAA&text=bin%3Aorganic-ugc_type_comments-gz_alpha&checksum=ptXaYE3YdQFw1DAFY2mDRiBz+rxwGU7ihVcV8tty1Wc%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида с пустым приветствием', function() {
            return this.browser
                .url('/turbo?checksum=Z4XODGLSIHgBtER%2FHKre6BUF3PWOW09fMmi8g999o%2FQ%3D&brand=organic-ugc&text=bin%3Aorganic-ugc_type_comments-gz_alpha&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2DtRIkIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOr3rnJInpFJPzq4b0%2FTXrhBpQeTmYOLQOtmMAGsr324chYmVESIAHlhHs%2FXxUmHz5otQ102s5Wzp1IP3wExdPJ8CV96R++SFAC0oE76LQRYt3hQbZ6hEpp1wvVJWNbMUJzRvMVzwUrii8d7ZvdhgEAAA%3D%3D')
                .assertView('plain', PO.Organic());
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида c очень длинным текстом', function() {
            return this.browser
                .url('/turbo?brand=organic-ugc&data=H4sIAAAAAAAAE22PwY6DIBCGnwZvNghi68FDrW3Sx2B1YokIBlDSPn0R18Rs9vLzzfDNEJxwEirUYFTma9Y1ajJ0eSCCY9nExLF53nnTIv%2FVQt4OzTBFIxcH7b4LWRTqw8L7fwvLqJHIl124xWT7VUj8K6x53Z5OZiOrl3OTRfSKSPjVw3t%2FmvTCDSg9nMwcWgZaMYENZF%2FahyNjZUZJgASUE+797CpMPnzQahvotZ2tnHuRfvgIiqeT4Uva6R++SFAC0oE76LURYt3hQbZ6hMprI7tkbCtGaM5ovuK5YEXxBTyF23yEAQAA&text=bin%3Abeauty_turbo_comments_very_long_text&checksum=ptXaYE3YdQFw1DAFY2mDRiBz+rxwGU7ihVcV8tty1Wc%3D')
                .assertView('plain', PO.Organic());
        });
    });

    describe('surveys page', function() {
        function loadPage(url) {
            return this.browser
                .url(url || '/turbo?data=H4sIAAAAAAAAE+2UzXKDIBSFnwZ2ZhBiTBYsNNGZPgZVWh3xJ4Jx2qcvgjok2TXJLpvjAbmXe+EbOvbNJSVQlUpwCk4IHNJJ4wCcfLBHxmPjY+MPACMQHifVw8ifTRwZdf3e6HH287LUSZsY3ZoZsgRqj51Ubrj+5S+B2odOnWuI9skyY9drTZdw5GS24fFS6prw5DQbXO/rnszO6XHt7r5Ns/ucdq3cd2reA5LCoRe0UKqTgEQA63LTcRw3CB02rNaDite8by+ttqxTvCq9avj9ZFIyr+s9wZuyYZ6Pwk2hagGLVipqYyFv9MX+fOT0n7lGLrK25vSr7KWykGBLCXZ6SO4u9U3JayjBz8UE33KCHwQFU8mztsktKsSgEmHnVIOrpt6ovA4V8lxUyC0q5EFUCBVMvyl1Rn1Mps822MFukAWdxLswMXDY83OZU6N25g+yfzwzsAYAAA==&checksum=AAQRo8HArQvA+DyolEPzToarR5cYZpS1lC0AzzDL5ys=&brand=organic-ugc&text=bin:organic-ugc_type_surveys_alpha-gz')
                .execute(() => {
                    window.Ya = {
                        ...(window.Ya || {}), ...{
                            Cmnt: {
                                api: {
                                    sendMC: () => {},
                                    fetchTree: () => Promise.resolve({ meta: {} }),
                                    addComment: () => Promise.resolve(),
                                },
                            },
                        },
                    };
                });
        }

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида', function() {
            return loadPage.call(this, '/turbo?brand=organic-ugc&checksum=qccmG2GE7TlIFKmApXJT4mkyc4TVACDpOSGZz%2B1ahhA%3D&data=H4sIAAAAAAAAA32RTW%2BEIBCGfw1cGj9AR%2BXAQReb9N5Lj1tlVxq%2FCri2%2F76ISVu7SS%2FPMO8MLwPI0Sr7%2BdRyLW%2FtOVDjZQr1Em0xGmSrGjXKCHeTsccOPDQc0iwlGYN9nbO8gBTP56s0nLhoO%2F7XZ15Mx7GW76rlBDLGgAFhADEkNHjpnuXbPMTYTNpyBBXKK0TppFupXURJuWmU3pRRr6p3Y28qPTkGq1HfibYfPwWrBrllIFAuNu3O81eT6wCB3YP0kiMRI0Y2VrVnhQRBxcmvhWfhlcyzetilPTwe9%2B29juWdIu6qzqKmqHCnp96sjL0MntQz9YyDo4%2BbYi%2FUoU9izwQvuuedtbNxt42idV3Df%2F96lX0zDZJflDb2C%2FZwSJAfAgAA&socialserp_reqid=1569959519550532-YhTejpm0&text=organic-ugc_type_surveys-gz')
                .assertView('plain', PO.SerpOrganicSurveys.content());
        });

        hermione.only.notIn('safari13');
        it('Проверка отправки пустого коммента', function() {
            return loadPage.call(this).click(PO.SerpOrganicSurveys.button())
                .yaWaitForVisible(PO.SerpOrganicSurveys.textArea())
                // Уводим фокус из контрола, чтобы отобразилось состояние ошибки.
                .click('body')
                .assertView('empty', PO.SerpOrganicSurveys.textArea());
        });

        hermione.only.notIn('safari13');
        it('Проверка отправки коммента', function() {
            return loadPage.call(this)
                .setValue(PO.SerpOrganicSurveys.textArea.control(), 'text')
                .click(PO.SerpOrganicSurveys.button())
                .assertView('second-page', PO.SerpOrganicSurveys.content());
        });

        hermione.only.notIn('safari13');
        it('Проверка ухода в конце опроса', function() {
            return loadPage.call(this)
                .setValue(PO.SerpOrganicSurveys.textArea.control(), 'text')
                .click(PO.SerpOrganicSurveys.button())
                .setValue(PO.SerpOrganicSurveys.textArea.control(), 'text2')
                .execute(() => window.location.href)
                .then(url => {
                    this.browser.click(PO.SerpOrganicSurveys.button())
                        .then(() => assert.notEqual(url, window.location.href));
                });
        });
    });

    describe('surveys page with skip button', function() {
        beforeEach(function() {
            return this.browser
                .url('/turbo?checksum=+hfePtDCra31W+zy39NWCzYKtBbgFGCacR2fhXJcyAo%3D&brand=organic-ugc&text=bin%3Aorganic-ugc_type_surveys_alpha-gz&data=H4sIAAAAAAAAE+2Uy3KDIBSGnwZ2ZrjEXBYsNIkzfQwaaWVEpYJx2qcv4mVIsmuSXTe%2FP8g5nAPfoPmnMIxCK60SDBwR2GeDpjE4YrBD3hPvU+%2F3gCCwPQzqhgmeTJp4Df3O62Hy07IsSHvyuvYzdA50ngSpwnD3C8+Bzm+DOpcQ50%2FzzLjeaTaHoyDzGJ7OpS4Jj0Gz8fW+4clsgh6X7u7b9LtPaZfKcVDzDtAMdq1ihbXaAJoA4srN+r5fIbRf8coNSlGJtrk0znJtRSmjsvt558bwSLeRErWseYTRdlXYSsGiMZaNsVDU7mK%2F33L2x1y9UOemEuxDtsaOkJCREhL0cLq71H9KXkMJeS4m5JYT8iAohBlxbup8RIV6VBISnGp81dQ%2FKq9DhT4XFXqLCn0QFcoUd29KdWaY0OGzjjdQd6Zgg0QXrjoBW%2FElc+Z1mjGl1AxDriW7yFw0vyc1DjDBBgAA')
                .execute(() => {
                    window.Ya = {
                        ...(window.Ya || {}), ...{
                            Cmnt: {
                                api: {
                                    sendMC: () => {},
                                    fetchTree: () => Promise.resolve({ meta: {} }),
                                    addComment: () => Promise.resolve(),
                                },
                            },
                        },
                    };
                });
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида', function() {
            return this.browser
                .assertView('plain', PO.SerpOrganicSurveys.content());
        });

        hermione.only.notIn('safari13');
        it('Проверка пропуска страницы', function() {
            return this.browser
                .setValue(PO.SerpOrganicSurveys.textArea.control(), 'text')
                .click(PO.SerpOrganicSurveys.skipButton())
                .getValue(PO.SerpOrganicSurveys.textArea.control())
                .then(value => assert.equal('', value, 'Поле ввода не очистилось'))
                .assertView('skip', PO.SerpOrganicSurveys.content());
        });
    });

    describe('Страница отзывов с превью видео', function() {
        beforeEach(function() {
            return this.browser
                .url('/turbo?brand=organic-ugc&checksum=ocA%2Bc5yzNdDUqhHV/4dC65kLa3QOgbu1EJC7TrQKgVE%3D&data=H4sIAAAAAAAAA82QQW7DIBBFTwObSLWBQPFiFraSSNnnAmk8rVHsmBqcxLcvJhbyIgfo5vOYz4w%2Bgzdv/HSsgXGR5wXTSupCC8UFU4w2vfMwnW81Pj%2BGkXYXkFu1ZaqQ1J5/0AEPp28gu5sa%2B8wOeDf4IOLwbdruWBOxezeWcOXx6YNLuNzlQaoyUZVIv3FZIpmomGk2dEgzugbogL%2BmBuquxgKjbvw6TRYhZqThuy0CCa3lJwltms9c7TerSx61fBlR5WZVqqLqVYW9HkUtlkHiQMehhcZ764gosyzt8d9s64Htpe%2BWzSxx/gDw20sUEQIAAA%3D%3D&socialserp_reqid=&text=organic-ugc_type_surveys-gz')
                .execute(() => {
                    window.Ya = {
                        ...(window.Ya || {}), ...{
                            Cmnt: {
                                api: {
                                    sendMC: () => {},
                                    fetchTree: () => Promise.resolve({ meta: {
                                        text: 'ЧТО Такое Кликбейт? В этом видео я подробно и надеюсь доступно объяснил вам что такое кликбейт.',
                                        url: 'https://yandex.ru/video/preview?filmId=12300918658983623161&text=%D0%BA%D0%BB%D0%B8%D0%BA%D0%B1%D0%B5%D0%B9%D1%82',
                                        image: 'https://avatars.mds.yandex.net/get-shinyserp/1045461/2a0000016d8723ec1c612300001a95d20523/largePreview',
                                        title: 'Что такое кликбейт?',
                                        host: 'yandex.ru',
                                    } }),
                                    addComment: () => Promise.resolve(),
                                },
                            },
                        },
                    };
                });
        });

        hermione.only.notIn('safari13');
        it('Проверка внешнего вида', function() {
            return this.browser
                .yaWaitForVisible(PO.SerpOrganicSurveys.VideoPreview.description(), 'Дополнительная информация не загрузилась')
                .assertView('plain', PO.SerpOrganicSurveys.content());
        });
    });
});
