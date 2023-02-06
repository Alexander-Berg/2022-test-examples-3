specs({
    feature: 'product-page',
}, () => {
    describe('Внешний вид с рекомендациями', function() {
        hermione.only.notIn('safari13');
        it('Без описания', function() {
            return this.browser
                .url('?stub=productpage/product-1-server.json')
                .yaWaitForVisible(PO.pageJsInited())
                /**
                * В проде/приемке у нас нет темплара, нет ручки для формы
                * Делаем костыль, чтобы скрывать паранжу в прогонах
                */
                .execute(function() {
                    var paranja = document.querySelector('.turbo-modal__paranja');

                    if (paranja) {
                        paranja.click();
                    }
                })
                .yaWaitForVisible(PO.blocks.productsCarousel())
                .assertView('plain', [PO.blocks.form(), PO.blocks.productsCarousel(), PO.blocks.footer()]);
        });

        hermione.only.notIn('safari13');
        it('С описанием', function() {
            return this.browser
                .url('?stub=productpage/product-2-server.json')
                .yaWaitForVisible(PO.pageJsInited())
                /**
                * В проде/приемке у нас нет темплара, нет ручки для формы
                * Делаем костыль, чтобы скрывать паранжу в прогонах
                */
                .execute(function() {
                    var paranja = document.querySelector('.turbo-modal__paranja');

                    if (paranja) {
                        paranja.click();
                    }
                })
                .yaWaitForVisible(PO.blocks.productsCarousel())
                .assertView('plain', [PO.blocks.accordion(), PO.blocks.productsCarousel(), PO.blocks.footer()]);
        });
    });

    hermione.only.notIn('safari13');
    it('Подскрол картинки в слайдере по параметру sliderimg в адресе страницы', function() {
        return this.browser
            .url('/turbo?text=https%3A%2F%2Fwww.logomebel.ru%2Fstulya-na-metallokarkase%2Fstul-kreslo-sonata-komfort.html&sliderimg=%2F%2Favatars.mds.yandex.net%2Fget-turbo%2F1778853%2Frth680dc13d5d42b65e47b1a86df7dc22fd%2F')
            .yaWaitForVisible(PO.blocks.sliderHydro())
            .yaGetElementIndex(PO.blocks.slider.activeItem()).then(function(activeItemIndex) {
                assert.deepEqual(activeItemIndex, 2, 'неверный индекс текущей картинки');
            })
            .yaGetElementIndex(PO.blocks.slider.dotsCarousel.activeItem()).then(function(activeDotIndex) {
                // индекс точек сдвигается на 1, т.к. для анимации в начало добавляется невидимая точка
                // @see https://github.yandex-team.ru/serp/turbo/blob/93f90c6bf781f2be4ec6b67e430ce60fe357a91d/platform/components/Slider/Dots/_View/Slider-Dots_View_Carousel.tsx#L24-L29
                assert.deepEqual(activeDotIndex, 3, 'неверный индекс текущей точки');
            });
    });

    hermione.only.notIn('safari13');
    it('Картинка в форме 1 клика', function() {
        return this.browser
            .url('?stub=productpage/with-new-img-in-form.json')
            .yaWaitForVisible(PO.pageJsInited())
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с бейджом безопасности', function() {
        return this.browser
            .url('/turbo?stub=productpage/product-1-with-safe.json&exp_flags=turboforms_endpoint=https://test-turbo-forms.common.yandex.ru/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', [
                PO.blocks.ecomHeader(),
                PO.ecomSecureTransactionNotice(),
                PO.blocks.breadcrumbs(),
            ], {
                ignoreElements: [
                    PO.blocks.ecomHeader(),
                    PO.blocks.breadcrumbs(),
                ],
            });
    });
});
