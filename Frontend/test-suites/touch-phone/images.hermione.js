'use strict';

const PO = require('../../page-objects/touch-phone/index').PO;

specs('Колдунщик картинок', function() {
    hermione.only.notIn('winphone', 'winphone не поддерживает target: _blank у ссылок');
    it('проверка ссылок', function() {
        const catsImagesLinksPrefix = 'http://yandex.ru/images/touch/search?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8';

        return this.browser
            .yaOpenSerp('text=котики&srcskip=ATOM_PROXY')
            .yaWaitForVisible(PO.imagesConstructor(), 'Колдунщик картинок не появился')
            .yaGetReqId()
            .then(reqId => {
                return '&lite=1&noreask=1&source=wiz' +
                    '&parent-reqid=' + reqId + '&thumb_suffix=n=24';
            })
            .then(postfix =>
                this.browser
                    .yaCheckSnippet(PO.imagesConstructor, {
                        title: {
                            url: {
                                href: 'http://yandex.ru/images/touch/search?text=котики' + postfix,
                                ignore: ['protocol']
                            },
                            baobab: {
                                path: '/$page/$main/$result[@wizard_name="images"]/title'
                            }
                        },
                        showcase: [{
                            thumb: {
                                selector: PO.imagesConstructor.firstImage(),
                                url: {
                                    href: catsImagesLinksPrefix +
                                        '&img_url=https%3A%2F%2Fwallbox.ru%2Fwallpapers%2Fmain%2F201620%2F37da0352f83' +
                                        'ab7b.jpg&rpt=imageajax' + postfix,
                                    ignore: ['protocol']
                                },
                                baobab: {
                                    path: '/$page/$main/$result[@wizard_name="images"]/showcase/"thumb/p0"'
                                }
                            }
                        },
                        {
                            thumb: {
                                selector: PO.imagesConstructor.secondImage(),
                                url: {
                                    href: catsImagesLinksPrefix +
                                        '&img_url=https%3A%2F%2Fbesthqwallpapers.com%2FUploads%2F20-12-2016' +
                                        '%2F11198%2Fcat-fluffy-cat-pets-cute-cats.jpg&rpt=imageajax' + postfix,
                                    ignore: ['protocol']
                                },
                                baobab: {
                                    path: '/$page/$main/$result[@wizard_name="images"]/showcase/"thumb/p1"'
                                }
                            }
                        },
                        {
                            thumb: {
                                selector: PO.imagesConstructor.thirdImage(),
                                url: {
                                    href: catsImagesLinksPrefix +
                                        '&img_url=https%3A%2F%2Fimages.wallpaperscraft.ru%2Fimage%2Fkotenok_' +
                                        'pushistyy_udivlenie_vzglyad_53047_2560x1600.jpg&rpt=imageajax' + postfix,
                                    ignore: ['protocol']
                                },
                                baobab: {
                                    path: '/$page/$main/$result[@wizard_name="images"]/showcase/"thumb/p2"'
                                }
                            }
                        },
                        {
                            thumb: {
                                selector: PO.imagesConstructor.fourthImage(),
                                url: {
                                    href: catsImagesLinksPrefix +
                                        '&img_url=https%3A%2F%2Fwww.nastol.com.ua%2Fpic%2F201207%2F1920x1200%2F' +
                                        'nastol.com.ua-27573.jpg&rpt=imageajax' +
                                        postfix,
                                    ignore: ['protocol']
                                },
                                baobab: {
                                    path: '/$page/$main/$result[@wizard_name="images"]/showcase/"thumb/p3"'
                                }
                            }
                        }
                        ]
                    })
            );
    });
});
