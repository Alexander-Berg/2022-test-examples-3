/* eslint-disable require-iget2*/
match(function() {
    return this.data.cmd === 'testTemplateForm';
})(

    block('app-main').mode('page-block')('p-test-template'),

    block('b-page').mode('title')('Изолированное тестирование шаблонов'));

block('p-test-template').content()(function() {
    return {
        block: 'l-page',
        mods: {
            layout: '16-60-16'
        },
        content: [
            {
                elem: 'row',
                left: true,
                right: true,
                center: [
                    {
                        block: 'b-page-title',
                        content: {
                            elem: 'title',
                            content: 'Изолированное тестирование шаблонов'
                        }
                    },
                    {
                        block: 'p-test-template',
                        elem: 'description',
                        content: [
                            'Страница предназначена для тестирования шаблонов в отрыве от логики ' +
                                'формирования данных на сервере.<br>',
                            'В поле ввода нужно поместить дамп данных интересующей страницы. ' +
                                'Для страниц, шаблонизируемых tt2, нужно еще заполнить поле template<br>',
                            'Получить дамп данных можно приведя страницу к нужному состоянию и ' +
                                'добавив в параметры запросы get_vars=1.<br>',
                            {
                                block: 'b-link',
                                url: u.getUrl('tmplProc', {
                                    tmpl: 'jslinks'
                                }),
                                content: 'Здесь'
                            },
                            ' есть ссылка на букмарклет VARS, ' +
                                'который умеет добавлять/убирать get_vars в параметрах запроса'
                        ]
                    },
                    {
                        tag: 'form',
                        attrs: {
                            method: 'POST',
                            id: 'form'
                        },
                        content: [
                            {
                                block: 'input',
                                mix: [{
                                    block: 'p-test-template',
                                    elem: 'form-row'
                                }],
                                mods: {
                                    type: 'textarea'
                                },
                                name: 'data',
                                id: 'data',
                                content: [
                                    {
                                        elem: 'control'
                                    }
                                ]
                            },
                            {
                                tag: 'input',
                                attrs: {
                                    type: 'hidden',
                                    name: 'cmd',
                                    value: 'testTemplateProcess'
                                }
                            },
                            {
                                tag: 'label',
                                block: 'p-test-template',
                                elem: 'form-row',
                                content: [
                                    'Template: ',
                                    {
                                        block: 'input',
                                        mix: [{
                                            block: 'p-test-template',
                                            elem: 'template-input'
                                        }],
                                        name: 'template',
                                        content: {
                                            elem: 'control'
                                        }
                                    }
                                ]
                            },
                            {
                                block: 'button',
                                mix: [{
                                    block: 'p-test-template',
                                    elem: 'submit'
                                }],
                                mods: {
                                    theme: 'action'
                                },
                                type: 'submit',
                                content: 'Отправить'
                            }
                        ]
                    }
                ]
            }
        ]
    };
});
/* eslint-enable require-iget2*/
