specs({
    feature: 'beru-gallery',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока по умолчанию', function() {
        return this.browser
            .url('/turbo?stub=berugallery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Проверка работоспособности', function() {
        return this.browser
            .url('/turbo?stub=berugallery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('active-first-slide', PO.page())
            // Включаем анимации иначе не будет работать event у компонента onTransitionEnd на который вызывается коллбэк
            .waitUntil(() => this.browser.execute(turnOnAnimation), 300)
            .click(PO.blocks.turboCardSliderBeru.secondBullet())
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru.secondSlide(), 'Должен быть виден второй слайд в скролбоксе')
            .assertView('active-second-slide', PO.page())
            .click(PO.blocks.turboCardSliderBeru())
            .yaWaitForVisible(PO.blocks.turboModalBeru.secondSlide(), 'Должен быть виден второй слайд в попапе')
            .assertView('active-second-slide-in-popup', PO.blocks.turboModalBeru())
            .click(PO.blocks.turboModalBeru.thirdBullet())
            .yaWaitForVisible(PO.blocks.turboModalBeru.thirdSlide(), 'Должен быть виден третий сладй в попапе')
            .assertView('active-third-slide-in-popup', PO.blocks.turboModalBeru())
            .click(PO.blocks.turboModalBeru.close())
            .yaWaitForHidden(PO.blocks.turboModalBeru(), 'Попап должен закрыться')
            .assertView('active-third-slide', PO.page());
    });

    hermione.only.in('iphone', 'touch команды поддерживаются только на мобильных user-agent');
    hermione.only.notIn('safari13');
    it('Проверка работоспособности в телефоне', function() {
        return this.browser
            .url('/turbo?stub=berugallery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('active-first-slide', PO.page())
            // Включаем анимации иначе не будет работать event у компонента onTransitionEnd на который вызывается коллбэк
            .waitUntil(() => this.browser.execute(turnOnAnimation), 300)
            .yaTouchScroll(PO.blocks.turboCardSliderBeru(), 200)
            .yaWaitForVisible(PO.blocks.turboCardSliderBeru.secondSlide(), 'Должен быть виден второй слайд в скролбоксе')
            .assertView('active-second-slide', PO.page())
            .click(PO.blocks.turboCardSliderBeru())
            .yaWaitForVisible(PO.blocks.turboModalBeru.secondSlide(), 'Должен быть виден второй слайд в попапе')
            .assertView('active-second-slide-in-popup', PO.blocks.turboModalBeru())
            .yaTouchScroll(PO.blocks.turboModalBeru.turboCardSliderBeru(), 200)
            .yaWaitForVisible(PO.blocks.turboModalBeru.thirdSlide(), 'Должен быть виден третий сладй в попапе')
            .assertView('active-third-slide-in-popup', PO.blocks.turboModalBeru())
            .click(PO.blocks.turboModalBeru.close())
            .yaWaitForHidden(PO.blocks.turboModalBeru(), 'Попап должен закрыться')
            .assertView('active-third-slide', PO.page());
    });
});

function turnOnAnimation() {
    const style = document.createElement('style');

    style.setAttribute('type', 'text/css');
    style.innerHTML = '*:not(.alert_visible):not(.autoload), *:after, *:before {transition: all 1ms ease !important;}';

    document.body.appendChild(style);

    return true;
}
