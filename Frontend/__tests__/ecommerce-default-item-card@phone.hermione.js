specs({
    feature: 'Дефолтная карточка товара',
}, () => {
    hermione.only.notIn('safari13');
    it('Основные проверки', function() {
        return this.browser
            .url('/turbo?stub=page%2Fnike.json&hermione_scroll=disable')
            .yaWaitForVisible(PO.page.result())
            .assertView('plain', PO.page.result())
            .yaIndexify(PO.blocks.form())
            .yaScrollPage(PO.blocks.firstForm())
            .getAttribute(PO.blocks.firstForm(), 'data-bem')
            .then(paramsString => {
                const formParams = JSON.parse(paramsString).form || {};

                assert.equal(formParams.targetInSeparateTab, undefined, 'Задан targetInSeparateTab для формы');
            })
            .yaWaitForVisible(PO.callModalButton())
            .click(PO.callModalButton())
            .yaWaitForVisible(PO.modal(), 'Модальное окно с формой не показалось')
            .execute(function() {
                document.querySelector('.page__container').className =
                    document.querySelector('.page__container').className.replace(/\bpage__container_lock-scroll\b/, '');
            })
            .assertView('modal', PO.modal())
            .yaScrollElement(PO.modal.container(), 0, 2000)
            .pause(200)
            .assertView('modal-bottom', PO.modal())
            .click(PO.modal.turboButtonClose(), 'Модально еокно с формой не скрылось')
            .yaWaitForHidden(PO.modal());
    });

    hermione.only.notIn('safari13');
    it('Ошибка оформления', function() {
        return this.browser
            .url('/turbo?stub=page%2Fnike.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page())
            .yaScrollPage(PO.blocks.form())
            .yaWaitForVisible(PO.callModalButton())
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .click(PO.callModalButton())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно с формой не показалось')
            .setValue(PO.firstOneClickForm.nameField.control(), 'error')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Адрес')
            .click(PO.firstOneClickForm.submit())
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderLoading(), 'Не показался статус-скрин со спинером')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderFail(), 'Не показался статус-скрин неудачного заказа')
            .click(PO.blocks.turboStatusScreenOrderFail.buttonClose())
            .yaWaitForHidden(PO.blocks.statusModal(), 'Не скрылся статус-скрин ошибки')
            .yaShouldBeVisible(PO.firstOneClickForm(), 'После закрытия статус-скрина ушли с формы');
    });

    it('Успешное оформление', function() {
        return this.browser
            .url('/turbo?stub=page%2Fnike.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page())
            .yaScrollPage(PO.blocks.form())
            .yaWaitForVisible(PO.callModalButton())
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .click(PO.callModalButton())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно с формой не показалось')
            .setValue(PO.firstOneClickForm.nameField.control(), 'test')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Адрес')
            .click(PO.firstOneClickForm.submit())
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderLoading(), 'Не показался статус-скрин со спинером')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderSuccess(), 'Не показался статус-скрин успешного заказа')
            .getText(PO.turboModal.content())
            .then(text=>assert(/Номер вашего заказа 1234/.test(text), 'Номер заказа не отобразился'))
            .back()
            .yaWaitForHidden(PO.blocks.statusModal(), 'Не скрылся статус-скрин успеха')
            .yaShouldNotBeVisible(PO.firstOneClickForm(), 'После закрытия статус-скрина не закрылась модалка формы');
    });

    hermione.only.notIn('safari13');
    it('Проверка связанности полей', function() {
        return this.browser
            .url('/turbo?stub=page%2Fnike.json')
            .execute(function() {
                window.hermione_fetch_params = [];
                window.fetch = function(url, params) {
                    window.hermione_fetch_params.push(params);
                };
            })
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .click(PO.callModalButton())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно не появилось')

            .setValue(PO.firstOneClickForm.nameField.control(), 'test')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Адрес')
            .click(PO.firstOneClickForm.submit())
            .execute(function() {
                return window.hermione_fetch_params.pop();
            }).then(params => {
                const formParams = JSON.parse(params.value.body);
                assert.equal(formParams.size, '116011', 'В форме передаем неправильный размер');
                assert.isTrue(typeof formParams.meta === 'string', 'В форме не передаем обязательный параметр meta');
                assert.isTrue(typeof formParams.sk === 'string', 'В форме не передаем обязательный параметр sk');
            })
            .back()
            .click(PO.stateSelect.control())
            .selectByValue(PO.stateSelect.control(), '116016')

            .click(PO.callModalButton())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно не появилось')

            .getText(PO.keyValue.state())
            .then(value => assert.strictEqual(value, 'US 8 (RU 37,5)', 'Связанный контейнер не поменял текст'))
            .click(PO.firstOneClickForm.submit())
            .execute(function() {
                return window.hermione_fetch_params.pop();
            }).then(params => {
                assert.equal(JSON.parse(params.value.body).size, '116016', 'После изменения размера он не поменялся в форме');
            });
    });

    hermione.only.notIn('safari13');
    it('Обрезание заголовка в карточке товара', function() {
        return this.browser
            .url('/turbo?stub=productpage/product-without-cart.json')
            .yaWaitForVisible(PO.page())
            .assertView('header', PO.header());
    });

    hermione.only.notIn('safari13');
    it('Обрезание заголовка в карточке товара с корзиной', function() {
        return this.browser
            .url('/turbo?stub=productpage/product-2-server.json')
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
            .yaWaitForVisible(PO.header())
            .assertView('header', PO.header());
    });

    hermione.only.notIn('safari13');
    it('Добавить во внешнюю корзину по ссылке', function() {
        return this.browser
            .url('/turbo?stub=page/ecom-item-card-to-outer-card.json')
            .getAttribute(PO.blocks.productAddToExternalCart(), 'target')
            .then(target => {
                assert.equal(target, '_blank', 'Не должен быть задан targetInSeparateTab для кнопки');
            });
    });

    describe('С экспериментом для маркета', function() {
        hermione.only.notIn('safari13');
        it('Переход в корзину через форму', function() {
            return this.browser
                .url('/turbo?stub=page/nike.json&ymclid=1&exp_flags=add-to-outer-cart-self-with-ymclid=1')
                .yaScrollPage(PO.blocks.form())
                .yaIndexify(PO.blocks.form())
                .getAttribute(PO.blocks.firstForm(), 'data-bem')
                .then(paramsString => {
                    const formParams = JSON.parse(paramsString).form || {};

                    assert.equal(formParams.targetInSeparateTab, '_self', 'Не задан targetInSeparateTab для формы');
                });
        });

        hermione.only.notIn('safari13');
        it('Переход в корзину по ссылке', function() {
            return this.browser
                .url('/turbo?stub=page/ecom-item-card-to-outer-card.json&ymclid=1&exp_flags=add-to-outer-cart-self-with-ymclid=1')
                .getAttribute(PO.blocks.productAddToExternalCart(), 'target')
                .then(target => {
                    assert.equal(target, '_self', 'Не задан targetInSeparateTab для кнопки');
                });
        });
    });
});
