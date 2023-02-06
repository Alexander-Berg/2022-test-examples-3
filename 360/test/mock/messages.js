before(function() {

    window.mock['messages'] = [
        {
            params: {},
            data: {
                details: {},
                message: [
                    {
                        "new": 1,
                        "date": {
                            "chunks": {},
                            "timestamp": "1330954517000",
                            "iso": "2012-03-05T17:35:17",
                            "short": "5 мар.",
                            "full": "5 мар. в 17:35"
                        },
                        "size": "488 КБ",
                        "last_status": "",
                        "flags": {
                            "attachment": ""
                        },
                        "type": [],
                        "subject": "Почему вполне выполнимо продвижение проекта?",
                        "firstline": "Рекламный блок, отбрасывая подробности, довольно неоднозначен. План размещения, не меняя концепции, изложенной выше, однообразно ускоряет анализ зарубежного опыта, не считаясь с затратами. Маркетинг",
                        "mid": "2190000000624510036",
                        "fid": "8493490",
                        "tid": "2190000001092063246",
                        "lid": ["2190000000152542517", "FAKE_ATTACHED_LBL", "FAKE_SEEN_LBL"],
                        "field": [
                            {
                                "name": "Забористый Пацанчик",
                                "email": "foginat6@yandex.ru",
                                "type": "from",
                                "ref": "ref"
                            },
                            {
                                "name": "foginat4@ya.ru",
                                "email": "foginat4@ya.ru",
                                "type": "to",
                                "ref": "ref"
                            },
                            {
                                "name": "foginat6@yandex.ru",
                                "email": "foginat6@yandex.ru",
                                "type": "reply-to",
                                "ref": "ref"
                            }
                        ]
                    },
                    {
                        "new": 0,
                        "date": {
                            "chunks": {},
                            "timestamp": "1330954517000",
                            "iso": "2012-03-05T17:35:17",
                            "short": "5 мар.",
                            "full": "5 мар. в 17:35"
                        },
                        "size": "488 КБ",
                        "last_status": "",
                        "flags": {
                            "attachment": ""
                        },
                        "type": [],
                        "subject": "Почему вполне выполнимо продвижение проекта?",
                        "firstline": "Рекламный блок, отбрасывая подробности, довольно неоднозначен. План размещения, не меняя концепции, изложенной выше, однообразно ускоряет анализ зарубежного опыта, не считаясь с затратами. Маркетинг",
                        "mid": "2190000000624510037",
                        "fid": "8493490",
                        "tid": "2190000001092063246",
                        "lid": ["2190000000152542517", "FAKE_ATTACHED_LBL", "FAKE_SEEN_LBL"],
                        "field": [
                            {
                                "name": "Забористый",
                                "email": "foginat5@yandex.ru",
                                "type": "from",
                                "ref": "ref"
                            },
                            {
                                "name": "foginat4@ya.ru",
                                "email": "foginat4@ya.ru",
                                "type": "to",
                                "ref": "ref"
                            },
                            {
                                "name": "foginat5@yandex.ru",
                                "email": "foginat5@yandex.ru",
                                "type": "reply-to",
                                "ref": "ref"
                            }
                        ]
                    }
                ]
            }
        },

        //есть одна страница списка и размер меньше пейджера
        {
            params: {
                current_folder: 'insertMessage1'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 5
                    }
                },
                message: [
                    {
                        mid: 'insertMessage1-1',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage1'
                    },
                    {
                        mid: 'insertMessage1-2',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage1'
                    },
                    {
                        mid: 'insertMessage1-3',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage1'
                    }
                ]
            }
        },

        //есть одна страница списка и размер меньше пейджера на 1
        {
            params: {
                current_folder: 'insertMessage2'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 4
                    }
                },
                message: [
                    {
                        mid: 'insertMessage2-1',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage2'
                    },
                    {
                        mid: 'insertMessage2-2',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage2'
                    },
                    {
                        mid: 'insertMessage2-3',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage2'
                    }
                ]
            }
        },

        //есть одна страница списка и размер равен пейджеру
        {
            params: {
                current_folder: 'insertMessage3'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 3
                    }
                },
                message: [
                    {
                        mid: 'insertMessage3-1',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage3'
                    },
                    {
                        mid: 'insertMessage3-2',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage3'
                    },
                    {
                        mid: 'insertMessage3-3',
                        date: {
                            chunks: {}
                        },
                        fid: 'insertMessage3'
                    }
                ]
            }
        },

        //есть несколько страниц списка и еще не загруженные страницы
        {
            params: {
                current_folder: 'insertMessage4'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 3
                    }
                },
                message: [
                    {
                        mid: 'insertMessage4-1',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-2',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-3',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },
        {
            params: {
                current_folder: 'insertMessage4',
                page_number: '2'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 3
                    }
                },
                message: [
                    {
                        mid: 'insertMessage4-4',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-5',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-6',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },
        {
            params: {
                current_folder: 'insertMessage4',
                page_number: '3'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 3
                    }
                },
                message: [
                    {
                        mid: 'insertMessage4-7',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-8',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessage4-9',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },

        //тредный список
        {
            params: {
                thread_id: 'insertMessageThread1'
            },
            data: {
                details: {
                    pager: {
                        'items-per-page': 3
                    }
                },
                message: [
                    {
                        mid: 'insertMessageThread1-1',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessageThread1-2',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'insertMessageThread1-3',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },

        //есть не только приветственные письма
        {
            params: {
                page_number: '113'
            },
            data: {
                details: {},
                message: [
                    {
                        mid: '122',
                        date: {
                            chunks: {}
                        },
                        fid: '003',
                        type: [12, 1]
                    },
                    {
                        mid: '121',
                        date: {
                            chunks: {}
                        },
                        fid: '004',
                        type: [1]
                    }
                ]
            }
        },

        //есть только приветственные письма
        {
            params: {
                page_number: '113'
            },
            data: {
                details: {},
                message: [
                    {
                        mid: '123',
                        date: {
                            chunks: {}
                        },
                        fid: '001',
                        type: [12, 33]
                    },
                    {
                        mid: '124',
                        date: {
                            chunks: {}
                        },
                        fid: '002',
                        type: [12, 22]
                    }
                ]
            }
        },

        //тред без входящих сообщений (только черновик, спам, отправленные и корзина)
        {
            params: {
                thread_id: 'nonvalidIncomeMessageThread1',
                sort: 'date'
            },
            data: {
                details: {},
                message: [
                    {
                        mid: 'mid01',
                        fid: 'sent',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid02',
                        fid: 'trash',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid03',
                        fid: 'spam',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid04',
                        fid: 'draft',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },

        //тред c входящими сообщениями
        {
            params: {
                thread_id: 'validIncomeMessageThread1',
                sort: 'date'
            },
            data: {
                details: {},
                message: [
                    {
                        mid: 'mid01',
                        fid: 'sent',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid02',
                        fid: 'trash',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid03',
                        fid: 'spam',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid04',
                        fid: 'draft',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid05',
                        fid: 'inbox',
                        date: {
                            chunks: {}
                        }
                    },
                    {
                        mid: 'mid06',
                        fid: 'archive',
                        date: {
                            chunks: {}
                        }
                    }
                ]
            }
        },

        {
            params: {
                current_folder: 'refresh1',
                sort_type: 'date',
                first: 0,
                count: 30,
                with_pins: 'yes'
            },
            data: {
                details: {},
                message: [
                    {
                        mid: 'mrefresh1',
                        date: {
                            chunks: {}
                        },
                        fid: 'refresh1'
                    },
                    {
                        mid: 'refresh2',
                        date: {
                            chunks: {}
                        },
                        fid: 'refresh1'
                    }
                ]
            }
        },
    ];

});
