'use strict';

const PO = require('../../../../page-objects/touch-phone/index').PO;

specs('Видео / Обычный (тайтл без шаблона)', function() {
    beforeEach(function() {
        return this.browser
            .yaOpenSerp('text=котики видео')
            .yaWaitForVisible(PO.videoWizard(), 'Должен появиться колдунщик видео');
    });

    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('Проверка ссылок и счётчиков', function() {
        const VIDEO_URL = {
            href: 'http://yandex.ru/video/touch/search',
            ignore: ['protocol', 'query']
        };

        return this.browser
            .yaCheckSnippet(PO.videoWizard, {
                title: {
                    url: VIDEO_URL,
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="video"]/title'
                    }
                },
                greenurl: [{
                    url: VIDEO_URL,
                    baobab: {
                        path: '/$page/$main/$result[@wizard_name="video"]/path/urlnav'
                    }
                }],
                showcase: [{
                    thumb: {
                        selector: PO.videoWizard.firstItemThumbLink(),
                        url: VIDEO_URL,
                        baobab: {
                            path: '/$page/$main/$result[@wizard_name="video"]/showcase/thumb'
                        }
                    },
                    descr: {
                        selector: PO.videoWizard.firstItemSubtitle(),
                        url: VIDEO_URL,
                        baobab: {
                            path: '/$page/$main/$result[@wizard_name="video"]/showcase/thumb'
                        }
                    },
                    greenUrl: {
                        selector: PO.videoWizard.firstItemGreenUrl(),
                        baobab: {
                            path: '/$page/$main/$result[@wizard_name="video"]/showcase/path/urlnav'
                        }
                    }
                }]
            });
    });
});
