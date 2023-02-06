({
    block: 'b-page',
    title: 'Slider',
    head: [
        {elem: 'css', url: '_gemini.css', ie: false},
        {elem: 'css', url: '_gemini', ie: true}
    ],
    content: [
        {
            block: 'gemini-simple',
            attrs: {style: 'margin: 20px; height: 90px; width: 600px;'},
            content: [
                {
                    block: 'slider',
                    mods: {theme: 'normal', size: 'm', orientation: 'horiz'},
                    js: {
                        scale: [
                            {value: 0, step: 10, label: '0'},
                            {value: 100, label: '100'}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                content: {elem: 'control'},
                                value: 50
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini-bg-color',
            attrs: {
                style: 'margin:20px; height:90px; width:600px; background:#f5f5ea; padding:5px; display:inline-block;'
            },
            content: {
                block: 'slider',
                mods: {theme: 'normal', size: 'm', orientation: 'horiz'},
                js: {
                    scale: [
                        {value: 0, step: 10, label: '0'},
                        {value: 100, label: '100'}
                    ]
                },
                content: {
                    elem: 'info',
                    elemMods: {preset: 'inline'},
                    content: [
                        {
                            elem: 'title',
                            content: 'Цена'
                        },
                        {
                            block: 'input',
                            mods: {size: 'm'},
                            content: {elem: 'control'},
                            value: 50
                        },
                        {
                            elem: 'unit',
                            content: 'руб.'
                        }
                    ]
                }
            }
        },
        {
            block: 'gemini-marks',
            attrs: {style: 'margin: 20px; width: 600px;'},
            content: [
                {
                    block: 'slider',
                    mods: {theme: 'normal', size: 'm', orientation: 'horiz'},
                    js: {
                        min: 10,
                        max: 300,
                        scale: [
                            {value: 0, step: 10, label: ' '},
                            {value: 10, step: 5, label: ' ', percent: 10},
                            {value: 40, step: 10, label: ' ', percent: 40},
                            {value: 300, step: 20, label: ' ', percent: 80},
                            {value: 500, label: ' '}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                content: {elem: 'control'},
                                value: 40
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                },
                {
                    block: 'gemini-marks-expander',
                    attrs: {style: 'height: 40px; margin-left: -20px; margin-right: -20px;'}
                }
            ]
        },
        {
            block: 'gemini-hidden',
            attrs: {style: 'margin: 20px; width: 600px;'},
            content: [
                {
                    block: 'slider',
                    mods: {theme: 'normal', size: 'm', orientation: 'horiz', input: 'hidden'},
                    js: {
                        min: 10,
                        max: 90,
                        scale: [
                            {value: 0, step: 10},
                            {value: 100}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Слайдер'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                value: 30,
                                content: {elem: 'control'}
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini-range',
            // Explicit height until gemini rounds sizes up
            attrs: {style: 'margin: 20px;height: 75px; width: 600px;'},
            content: [
                {
                    block: 'slider',
                    mods: {type: 'range', theme: 'normal', size: 'm', orientation: 'horiz'},
                    js: {
                        min: 10,
                        max: 90,
                        scale: [
                            {value: 0, step: 10},
                            {value: 100}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                value: 30,
                                content: {elem: 'control'}
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                value: 70,
                                content: {elem: 'control'}
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini-disabled',
            attrs: {style: 'width: 600px;'},
            content: [
                {
                    block: 'slider',
                    // Explicit height to make gemini capture bottom border
                    attrs: {style: 'margin: 20px;height: 80px'},
                    mods: {theme: 'normal', size: 'm', orientation: 'horiz', disabled: 'yes'},
                    js: {
                        scale: [
                            {value: 0, step: 10},
                            {value: 100}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                content: {elem: 'control'},
                                value: 50
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                },
                {
                    block: 'slider',
                    attrs: {style: 'margin: 20px'},
                    mods: {
                        type: 'range',
                        theme: 'normal',
                        size: 'm',
                        orientation: 'horiz',
                        disabled: 'yes',
                        input: 'hidden'
                    },
                    js: {
                        min: 10,
                        max: 90,
                        scale: [
                            {value: 0, label: '0', step: 10},
                            {value: 100, label: '100'}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                value: 30,
                                content: {elem: 'control'}
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                value: 70,
                                content: {elem: 'control'}
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                }
            ]
        },
        {
            block: 'gemini-size',
            attrs: {style: 'margin: 20px; width: 600px;'},
            content: {
                block: 'b-layout-table',
                mods: {layout: '30-70'},
                content: [
                    {
                        elem: 'row',
                        content: [{
                            elem: 'cell',
                            content: {
                                block: 'button',
                                mods: {size: 'xs', theme: 'normal'},
                                url: '#',
                                content: 'Размер XS'
                            }
                        },
                            {
                                elem: 'cell',
                                elemMods: {position: 'r'},
                                content: [
                                    {
                                        attrs: {style: 'margin: 20px'},
                                        content: {
                                            block: 'slider',
                                            mods: {
                                                theme: 'normal',
                                                size: 'xs',
                                                orientation: 'horiz',
                                                input: 'hidden'
                                            },
                                            js: {
                                                min: 10,
                                                max: 90,
                                                scale: [
                                                    {value: 0, step: 40},
                                                    {value: 100}
                                                ]
                                            },
                                            content: {
                                                elem: 'info',
                                                elemMods: {preset: 'inline'},
                                                content: [
                                                    {
                                                        elem: 'title',
                                                        content: 'Цена'
                                                    },
                                                    {
                                                        block: 'input',
                                                        mods: {size: 'xs'},
                                                        value: 70,
                                                        content: {elem: 'control'}
                                                    },
                                                    {
                                                        elem: 'unit',
                                                        content: 'руб.'
                                                    }
                                                ]

                                            }
                                        }
                                    }

                                ]
                            }
                        ]
                    },
                    {
                        elem: 'row',
                        content: [{
                            elem: 'cell',
                            content: {
                                block: 'button',
                                mods: {size: 's', theme: 'normal'},
                                url: '#',
                                content: 'Размер S'
                            }
                        },
                            {
                                elem: 'cell',
                                elemMods: {position: 'r'},
                                content: [
                                    {
                                        attrs: {style: 'margin: 20px'},
                                        content: {
                                            block: 'slider',
                                            mods: {
                                                theme: 'normal',
                                                size: 's',
                                                orientation: 'horiz',
                                                input: 'hidden'
                                            },
                                            js: {
                                                min: 10,
                                                max: 90,
                                                scale: [
                                                    {value: 0, step: 40},
                                                    {value: 100}
                                                ]
                                            },
                                            content: {
                                                elem: 'info',
                                                elemMods: {preset: 'inline'},
                                                content: [
                                                    {
                                                        elem: 'title',
                                                        content: 'Цена'
                                                    },
                                                    {
                                                        block: 'input',
                                                        mods: {size: 's'},
                                                        value: 70,
                                                        content: {elem: 'control'}
                                                    },
                                                    {
                                                        elem: 'unit',
                                                        content: 'руб.'
                                                    }
                                                ]

                                            }
                                        }
                                    }

                                ]
                            }
                        ]
                    },
                    {
                        elem: 'row',
                        content: [
                            {
                                elem: 'cell',
                                content: {
                                    block: 'button',
                                    mods: {size: 'm', theme: 'normal'},
                                    url: '#',
                                    content: 'Размер M'
                                }
                            },
                            {
                                elem: 'cell',
                                elemMods: {position: 'r'},
                                content: [
                                    {
                                        attrs: {style: 'margin: 20px'},
                                        content: {
                                            block: 'slider',
                                            mods: {
                                                theme: 'normal',
                                                size: 'm',
                                                orientation: 'horiz',
                                                input: 'hidden'
                                            },
                                            js: {
                                                min: 10,
                                                max: 90,
                                                scale: [
                                                    {value: 0, step: 50},
                                                    {value: 300}
                                                ]
                                            },
                                            content: {
                                                elem: 'info',
                                                elemMods: {preset: 'inline'},
                                                content: [
                                                    {
                                                        elem: 'title',
                                                        content: 'Цена'
                                                    },
                                                    {
                                                        block: 'input',
                                                        mods: {size: 'm'},
                                                        value: 40,
                                                        content: {elem: 'control'}
                                                    },
                                                    {
                                                        elem: 'unit',
                                                        content: 'руб.'
                                                    }
                                                ]

                                            }
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        },
        {
            block: 'gemini-contained',
            attrs: {style: 'margin: 20px; height: 90px; width: 600px;'},
            content: [
                {
                    block: 'slider',
                    mods: {theme: 'normal', size: 'm', orientation: 'horiz', contained: 'yes'},
                    js: {
                        scale: [
                            {value: 0, step: 10, label: '0'},
                            {value: 100, label: '100'}
                        ]
                    },
                    content: {
                        elem: 'info',
                        elemMods: {preset: 'inline'},
                        content: [
                            {
                                elem: 'title',
                                content: 'Цена'
                            },
                            {
                                block: 'input',
                                mods: {size: 'm'},
                                content: {elem: 'control'},
                                value: 50
                            },
                            {
                                elem: 'unit',
                                content: 'руб.'
                            }
                        ]
                    }
                }
            ]
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {elem: 'js', url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'}
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
