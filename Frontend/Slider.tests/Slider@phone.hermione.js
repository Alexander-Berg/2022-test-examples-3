specs({
    feature: 'Slider',
}, () => {
    it('Проверка автоперелистывания слайдов', async function() {
        await this.browser.url('/turbo?stub=slider/ecom-banner.json');
        await this.browser.yaWaitForVisible(PO.blocks.sliderAutomoveEnabled());

        let activeItemIndex1;
        await this.browser.yaWaitUntil('Неверный индекс первого слайда', async() => {
            activeItemIndex1 = await getActiveItemIndex.call(this);
            return activeItemIndex1 > -1;
        }, 1000);

        await this.browser.yaWaitUntil('Слайд не перелистнулся', async() => {
            const activeItemIndex2 = await getActiveItemIndex.call(this);
            return activeItemIndex2 > -1 && activeItemIndex1 !== activeItemIndex2;
        }, 3000, 500);
    });

    it('Остановка автоперелистывания при взаимодействии', async function() {
        await this.browser.url('/turbo?stub=slider/ecom-banner.json');
        await this.browser.yaWaitForVisible(PO.blocks.sliderAutomoveEnabled());

        await this.browser.yaTouchScroll(PO.slider.content(), 200);

        await this.browser.yaWaitForVisible(PO.blocks.sliderAutomoveDisabled());

        let activeItemIndex1;
        await this.browser.yaWaitUntil('Неверный индекс первого слайда', async() => {
            activeItemIndex1 = await getActiveItemIndex.call(this);
            return activeItemIndex1 > -1;
        }, 1000);

        // Ждём, например, 2 секунды, вдруг слайд перелистнётся.
        await this.browser.pause(2000);

        const activeItemIndex2 = await getActiveItemIndex.call(this);
        assert.strictEqual(activeItemIndex1, activeItemIndex2, 'Слайд не должен был поменяться');
    });

    hermione.only.in('chrome-phone', 'touch команды поддерживаются только на реальных устройствах');
    it('С одним слайдом не должно быть свайпа', function() {
        return this.browser
            .url('?stub=slider/single-item.json')
            .yaWaitForVisible(PO.slider())
            .assertView('plain', PO.slider())
            .yaShouldNotBeVisible(PO.slider.dotsCarousel(), 'Видна карусель с точками')
            .yaShouldNotBeVisible(PO.slider.count(), 'Виден номер слайда')
            // Инитим слайдер https://st.yandex-team.ru/SERP-63150
            .yaTouchScroll(PO.slider.content.items(), 2)
            .yaTouchScroll(PO.slider.content(), 200)
            .getCssProperty(PO.slider.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Слайдер двигался');
            });
    });

    it('Внешний вид для банеров в ecom', function() {
        return this.browser
            .url('?stub=slider/for-banner.json')
            .yaWaitForVisible(PO.slider())
            .yaIndexify(PO.blocks.sliderHydro())
            .assertView('plain', [
                PO.slider0(),
                PO.slider1(),
            ]);
    });

    hermione.only.in('chrome-phone', 'touch команды поддерживаются только на реальных устройствах');
    it('Проверка работы слайдера', function() {
        let currentTransform;

        return this.browser
            .url('?text=test_news')
            .yaWaitForVisible(PO.sliderSquare())
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Номер активного слайда должен быть 1');
            })
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.firstActiveItem(), 'Первая точка в навигации слайдера не активна')
            .yaIndexify(PO.slider.item())
            .getCssProperty(PO.slider.thirdItem.videoThumb(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде загружена');
            })
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в четвертом слайде загружена');
            })
            // Инитим слайдер https://st.yandex-team.ru/SERP-63150
            .yaTouchScroll(PO.sliderSquare.content.items(), 2)
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                currentTransform = prop.value;
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.firstActiveItem(),
                'Первая точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна')
            .getCssProperty(PO.slider.thirdItem.videoThumb(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде не загружена');
            })
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда не поменялся на 3');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.notEqual(prop.value, currentTransform, 'Третий слайд не стал видимым');
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.secondActiveItem(),
                'Вторая точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.thirdActiveItem(), 'Третья точка в навигации слайдера не активна')
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в четвертом слайде не загружена');
            })
            .yaTouchScroll(PO.sliderSquare.content(), -200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд не стал видимым');
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.thirdActiveItem(),
                'Третья точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна')
            .yaTouchScroll(PO.sliderSquare.content(), 100, 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда поменялся');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд не остался видимым');
            })
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна');
    });

    it('Проверка внешнего вида слайдера с рекламой (стаб)', function() {
        return this.browser
            .url('?stub=slider/with-advert.json&hermione_advert=stub&load-react-advert-script=1&exp_flags=adv-disabled=0')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForHidden(PO.slider.firstItemAdvertLoader(), 5000, 'Не загрузилась реклама')
            .yaWaitForVisible(PO.slider.firstItemAdvert(), 'Не появилась реклама')
            .assertView('plain', PO.sliderSquare(), { ignoreElements: ['.embed'] });
    });

    it('Проверка внешнего вида слайдера с рекламой (без стаба)', function() {
        return this.browser
            .url('?stub=slider/with-advert.json&hermione_advert=stub')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.sliderSquare(), 'Не показалась страница со слайдером')
            .yaWaitAdvert(PO, 'Реклама не загрузилась')
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Номер активного слайда должен быть 1');
            })
            // Инитим слайдер https://st.yandex-team.ru/SERP-63150
            .yaTouchScroll(PO.sliderSquare.content.items(), 2)
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            })
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда не поменялся на 3');
            })
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '4', 'Номер активного слайда не поменялся на 4');
            })
            .yaTouchScroll(PO.sliderSquare.content(), -200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда не поменялся на 3');
            })
            .yaTouchScroll(PO.sliderSquare.content(), -200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            });
    });

    it('Проверка загрузки слайдера со сложным содержимым', function() {
        return this.browser
            .url('?stub=slider/with-complicated-content.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.turboSliderSquare(), 'Не показалась страница со слайдером');
    });

    hermione.only.in('chrome-phone', 'touch команды поддерживаются только на реальных устройствах');
    it('Проверка работы слайдера с блоком video-thumb на Реакте', function() {
        let currentTransform;

        return this.browser
            .url('?text=test_news&exp_flags=force-react-video-thumb=1')
            .yaWaitForVisible(PO.sliderSquare())
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '1', 'Номер активного слайда должен быть 1');
            })
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.firstActiveItem(), 'Первая точка в навигации слайдера не активна')
            .yaIndexify(PO.slider.item())
            .getCssProperty(PO.slider.thirdItem.turboVideoThumb(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде загружена');
            })
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.equal(prop.value, 'none', 'Картинка в четвертом слайде загружена');
            })
            // Инитим слайдер https://st.yandex-team.ru/SERP-63150
            .yaTouchScroll(PO.sliderSquare.content.items(), 2)
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                currentTransform = prop.value;
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.firstActiveItem(),
                'Первая точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна')
            .getCssProperty(PO.slider.thirdItem.turboVideoThumb(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в видеотумбе в третьем слайде не загружена');
            })
            .yaTouchScroll(PO.sliderSquare.content(), 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '3', 'Номер активного слайда не поменялся на 3');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.notEqual(prop.value, currentTransform, 'Третий слайд не стал видимым');
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.secondActiveItem(),
                'Вторая точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.thirdActiveItem(), 'Третья точка в навигации слайдера не активна')
            .getCssProperty(PO.slider.fourthItem.image(), 'background-image')
            .then(prop => {
                assert.notEqual(prop.value, 'none', 'Картинка в четвертом слайде не загружена');
            })
            .yaTouchScroll(PO.sliderSquare.content(), -200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда не поменялся на 2');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд не стал видимым');
            })
            .yaShouldNotBeVisible(
                PO.sliderSquare.dotsCarousel.thirdActiveItem(),
                'Третья точка в навигации слайдера активна'
            )
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна')
            .yaTouchScroll(PO.sliderSquare.content(), 100, 200)
            .getText(PO.sliderSquare.count.activeItemNumber())
            .then(text => {
                assert.equal(text, '2', 'Номер активного слайда поменялся');
            })
            .getCssProperty(PO.sliderSquare.content.items(), 'transform')
            .then(prop => {
                assert.equal(prop.value, currentTransform, 'Второй слайд не остался видимым');
            })
            .yaShouldBeVisible(PO.sliderSquare.dotsCarousel.secondActiveItem(), 'Вторая точка в навигации слайдера не активна');
    });

    describe('Проверка внешнего вида слайдера', function() {
        const commonCgiParams = '&exp_flags=image-simple-lazy=0';
        const portraitOrientationTestCases = [
            'slider/with-video',
            'slider/with-video-drop-label',
            'slider/with-full-promo',
            'slider/with-title-promo',
            'slider/indented',
            'slider/theme-white',
        ];
        const bothOrientationTestCases = [
            'slider/square-cover',
            'slider/square-contain',
            'slider/portrait-cover',
            'slider/portrait-contain',
            'slider/landscape-cover',
            'slider/landscape-contain',
        ];

        portraitOrientationTestCases.forEach(testCase => {
            it(testCase, function() {
                return this.browser
                    .url(`?stub=${testCase}.json${commonCgiParams}`)
                    .yaWaitForVisible(PO.slider(), 'Не показалась страница со слайдером')
                    .execute(ensureBlocksVisibility, PO.slider(), PO.page.result())
                    .assertView('plain', PO.slider());
            });
        });

        bothOrientationTestCases.forEach(testCase => {
            hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
            it(testCase, function() {
                return this.browser
                    .url(`?stub=${testCase}.json${commonCgiParams}`)
                    .yaWaitForVisible(PO.slider(), 'Не показалась страница со слайдером')
                    .execute(ensureBlocksVisibility, PO.slider(), PO.page.result())
                    .assertView('portrait', PO.slider())
                    .setOrientation('landscape')
                    .execute(ensureBlocksVisibility, PO.slider(), PO.page.result())
                    .assertView('landscape', PO.slider());
            });
        });
    });
});

async function getActiveItemIndex() {
    const { value } = await this.browser.execute(function(itemSelector, activeItemSelector) {
        const list = Array.from(document.querySelectorAll(itemSelector));
        const active = document.querySelector(activeItemSelector);
        return list.indexOf(active);
    }, PO.blocks.slider.item(), PO.blocks.slider.activeItem());

    return value;
}

function ensureBlocksVisibility(targetBlockSelector, blockForMarginSelector) {
    const SAFE_THRESHOLD = 50;
    const targetBlock = document.querySelector(targetBlockSelector);
    const blockForMargin = document.querySelector(blockForMarginSelector);

    if (targetBlock.getBoundingClientRect().height > window.innerHeight - SAFE_THRESHOLD) {
        const excess = targetBlock.getBoundingClientRect().height - window.innerHeight;
        const padding = Math.min(excess, SAFE_THRESHOLD);
        blockForMargin.style.paddingBottom = `${padding}px`;
    }
}
