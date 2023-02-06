([{
    block: 'x-page',
    title: 'i-font',
    content: [
        {
            block: 'gemini',
            js: true,
            content: [
                {
                    block: 'gemini',
                    mix: [{block: 'gemini-fonts-block'}, {block: 'gemini-fonts-textbook'}],
                    content: ['textbook', 'textbook-new-bold', 'textbook-new-light'].map(function(face) {
                        return {
                            block: 'gemini',
                            mix: {block: face},
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [{block: 'i-font', mods: {face: face}}, {block: 'gemini-font-400-normal'}],
                                    content: [
                                        'йцукенгшщзфы вапрол',
                                        {tag: 'br'},
                                        'джжжэёячсмитьбюБЮ?',
                                        {tag: 'br'},
                                        '!"№%:,.;()ЙЦУКЕНГШЩЗХЪФЫВАПР',
                                        {tag: 'br'},
                                        'ОЛДЖЭЁЯЧСМИТЬБЮ]['
                                    ]
                                },
                                {
                                    block: 'gemini',
                                    mix: [{block: 'i-font', mods: {face: face}}, {block: 'gemini-font-400-normal'}],
                                    content: [
                                        '1234567890- =+*/qwertyuiop[]a',
                                        {tag: 'br'},
                                        'sdfghjkl;\'\\`',
                                        {tag: 'br'},
                                        'zxcvbnm,./QWERTYUIO P\u007b\u007dASDFGHJKL:"|~ZXCVBNM\u003c\u003e?±§'
                                    ]
                                }
                            ]
                        };
                    })
                },

                {
                    block: 'gemini',
                    mix: [{block: 'gemini-fonts-block'}, {block: 'gemini-fonts-konkord'}],
                    content: [
                        {
                            block: 'gemini',
                            mix: [{block: 'i-font', mods: {face: 'konkord'}}, {block: 'gemini-font-400-normal'}],
                            content: [
                                'йцукенгшщзфы вапрол',
                                {tag: 'br'},
                                'джжжэёячсмитьбюБЮ? !"№%:,.;()ЙЦУКЕНГШЩЗХЪФЫВАПР',
                                {tag: 'br'},
                                'ОЛДЖЭЁЯЧСМИТЬБЮ]['
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'i-font', mods: {face: 'konkord'}}, {block: 'gemini-font-400-normal'}],
                            content: [
                                '1234567890- =+*/qwertyuiop[]a',
                                {tag: 'br'},
                                'sdfghjkl;\'\\`',
                                {tag: 'br'},
                                'zxcvbnm,./QWERTYUIO P\u007b\u007dASDFGHJKL :"|~ZXCVBNM\u003c\u003e?±§'
                            ]
                        }
                    ]
                },

                {
                    block: 'gemini',
                    mix: [{block: 'gemini-fonts-block'}, {block: 'gemini-fonts-yandex'}],
                    content: [
                        {
                            block: 'gemini',
                            mix: [{block: 'i-font', mods: {face: 'yandex-ru'}}, {block: 'gemini-font-400-normal'}],
                            content: [
                                'йцукенгшщзфы вапрол',
                                {tag: 'br'},
                                'джжжэёячсмитьбюБЮ? !"№%:,.;()ЙЦУКЕНГШЩЗХЪФЫВАПР',
                                {tag: 'br'},
                                'ОЛДЖЭЁЯЧСМИТЬБЮ]['
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'i-font', mods: {face: 'yandex-en'}}, {block: 'gemini-font-400-normal'}],
                            content: [
                                '1234567890- =+*/qwertyuiop[]a',
                                {tag: 'br'},
                                'sdfghjkl;\'\\`',
                                {tag: 'br'},
                                'zxcvbnm,./QWERTYUIO P\u007b\u007dASDFGHJKL:"|~ZXCVBNM\u003c\u003e?±§'
                            ]
                        }
                    ]
                },

                {
                    block: 'gemini',
                    mix: {block: 'gemini-yandex-subsets'},
                    content: [
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-en-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Английский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        '1234567890- =+*/qw',
                                        {tag: 'br'},
                                        'ertyuiop[]asdfghjkl;\'\\`',
                                        {tag: 'br'},
                                        'zxcvbnm,./QWERTYUIO P\u007b\u007dASDFGH',
                                        {tag: 'br'},
                                        'JKL:"|~ZXCVBNM\u003c\u003e?±§'
                                    ]
                                }
                            ]
                        },

                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-ru-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Русский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'йцукенгшщзф ывапрол',
                                        {tag: 'br'},
                                        'джжжэёячсмитьб',
                                        {tag: 'br'},
                                        'юБЮ?!"№%:,.;()',
                                        {tag: 'br'},
                                        'ЙЦУКЕНГШ ЩЗХЪФЫВАПР',
                                        {tag: 'br'},
                                        'ОЛДЖЭЁЯЧ СМИТЬБЮ]['
                                    ]
                                }
                            ]
                        },

                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-tr-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Турецкий'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'AaBbCcÇçDd EeFfGgĞğHhIıİ',
                                        {tag: 'br'},
                                        'iJjKkLlMmNnOoÖöPp RrSsŞşTtUuÜüVvYyZz'
                                    ]
                                }
                            ]
                        },

                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-ua-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Украинский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'АаБбВвГгҐґ ДдЕеЄєЖжЗзИи',
                                        {tag: 'br'},
                                        'ІіЇїЙйКкЛлМмНнО ',
                                        {tag: 'br'},
                                        'оПпРрСсТтУуФфХ хЦцЧчШшЩщЬьЮюЯя'
                                    ]
                                }
                            ]
                        },

                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-kz-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Казахский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: 'ӘәБбВвГгҒғДд ЕеЁёЖжЗзИиЙйКкҚқЛлМ мНнҢңОоӨөПпРр' +
                                    ' СсТтУуҰұҮ үФфХхҺһЦцЧчШ шЩщЪъЫыІіЬь'
                                }
                            ]
                        },

                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-br-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Белорусский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-text-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: 'АаБбВвГгДдЕеЁёЖж ЗзІіЙйКкЛлМмНнОоПпРр СсТтУуЎўФфХхЦцЧчШш ЫыЬьЭэЮюЯя'
                                }
                            ]
                        }
                    ]
                },

                {
                    block: 'gemini',
                    mix: [
                        {block: 'i-font', mods: {face: 'rub-arial-regular'}},
                        {block: 'gemini-fonts-block'},
                        {block: 'gemini-fonts-rub'}
                    ],
                    content: ['12px', '16px', '20px'].map(function(size) {
                        return {
                            tag: 'span',
                            attrs: {style: 'font-size:' + size + ';'},
                            content: 'Р \u00a0'
                        };
                    })
                },

                {
                    block: 'gemini',
                    mix: {block: 'gemini-yandex-display'},
                    content: [
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-en-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Английский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        '1234567890- =+*/qw',
                                        {tag: 'br'},
                                        'ertyuiop[]asdfghjkl;\'\\`',
                                        {tag: 'br'},
                                        'zxcvbnm,./QWERTYUIO P\u007b\u007dASDFGH',
                                        {tag: 'br'},
                                        'JKL:"|~ZXCVBNM\u003c\u003e?±§'
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-ru-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Русский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'йцукенгшщзф ывапрол',
                                        {tag: 'br'},
                                        'джжжэёячсмитьб',
                                        {tag: 'br'},
                                        'юБЮ?!"№%:,.;()',
                                        {tag: 'br'},
                                        'ЙЦУКЕНГШ ЩЗХЪФЫВАПР',
                                        {tag: 'br'},
                                        'ОЛДЖЭЁЯЧ СМИТЬБЮ]['
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-tr-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Турецкий'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'AaBbCcÇçDd EeFfGgĞğHhIıİ',
                                        {tag: 'br'},
                                        'iJjKkLlMmNnOoÖöPp RrSsŞşTtUuÜüVvYyZz'
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-ua-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Украинский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: [
                                        'АаБбВвГгҐґ ДдЕеЄєЖжЗзИи',
                                        {tag: 'br'},
                                        'ІіЇїЙйКкЛлМмНнО ',
                                        {tag: 'br'},
                                        'оПпРрСсТтУуФфХ хЦцЧчШшЩщЬьЮюЯя'
                                    ]
                                }
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-kz-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Казахский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: 'ӘәБбВвГгҒғДд ЕеЁёЖжЗзИиЙйКкҚқЛлМ мНнҢңОоӨөПпРр' +
                                    ' СсТтУуҰұҮ үФфХхҺһЦцЧчШ шЩщЪъЫыІіЬь'
                                }
                            ]
                        },
                        {
                            block: 'gemini',
                            mix: [{block: 'yandex-sans-br-subset'}, {block: 'gemini-fonts-misc'}],
                            content: [
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-thin'}},
                                        {block: 'gemini-font-100-normal'}
                                    ],
                                    content: 'Белорусский'
                                },
                                {
                                    block: 'gemini',
                                    mix: [
                                        {block: 'i-font', mods: {face: 'yandex-sans-display-regular'}},
                                        {block: 'gemini-font-400-normal'}
                                    ],
                                    content: 'АаБбВвГгДдЕеЁёЖж ЗзІіЙйКкЛлМмНнОоПпРр СсТтУуЎўФфХхЦцЧчШш ЫыЬьЭэЮюЯя'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
}]);
