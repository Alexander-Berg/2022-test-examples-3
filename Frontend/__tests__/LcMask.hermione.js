// TODO: поправить маски в Firefox (LPC-6781)
hermione.skip.in(['firefox'], 'Маски в Firefox работают неправильно');
specs({
    feature: 'LcMask',
}, () => {
    hermione.only.notIn('safari13');
    it('Круг', function() {
        return this.browser
            .url('/turbo?stub=lcmask/circle.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Квадрат', function() {
        return this.browser
            .url('/turbo?stub=lcmask/square.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Треугольник', function() {
        return this.browser
            .url('/turbo?stub=lcmask/triangle.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Параллелограмм', function() {
        return this.browser
            .url('/turbo?stub=lcmask/parallelogram.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Ромб', function() {
        return this.browser
            .url('/turbo?stub=lcmask/rhombus.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Пятиугольник', function() {
        return this.browser
            .url('/turbo?stub=lcmask/pentagon.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Шестиугольник', function() {
        return this.browser
            .url('/turbo?stub=lcmask/hexagon.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Восьмиугольник', function() {
        return this.browser
            .url('/turbo?stub=lcmask/octagon.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Флаг налево', function() {
        return this.browser
            .url('/turbo?stub=lcmask/flag-left.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Флаг направо', function() {
        return this.browser
            .url('/turbo?stub=lcmask/flag-right.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Стрелка налево', function() {
        return this.browser
            .url('/turbo?stub=lcmask/arrow-left.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Стрелка направо', function() {
        return this.browser
            .url('/turbo?stub=lcmask/arrow-right.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Рамка', function() {
        return this.browser
            .url('/turbo?stub=lcmask/frame.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Сообщение', function() {
        return this.browser
            .url('/turbo?stub=lcmask/message.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Крест', function() {
        return this.browser
            .url('/turbo?stub=lcmask/cross.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });

    hermione.only.notIn('safari13');
    it('Нет', function() {
        return this.browser
            .url('/turbo?stub=lcmask/none.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcPicture.imageLoaded(), 'Изображение не загрузилось')
            .assertView('plain', PO.lcImageLpc.image());
    });
});
