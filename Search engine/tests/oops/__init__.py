"""
    Package with test for OOPS API and module,
    in this file we define test responses and other simulation data
"""

UNIT_TEST_ARGS = (
    (
        {'fqdns': 'test1.search', 'attr_names': 'bot_status'},
        {}
    ),
    (
        {'fqdns': 'test1.search,', 'attr_names': 'bot_status'},
        {}
    ),
    (
        {'fqdns': 'test1.search, test2.search', 'attr_names': 'bot_status'},
        {}
    ),
    (
        {'fqdns': 'test1.search', 'attr_names': 'bot_status,'},
        {}
    ),
    (
        {'fqdns': 'test1.search', 'attr_names': 'bot_status,mem_info'},
        {}
    ),
    (
        {'fqdns': 'test1.search,test2.search', 'attr_names': 'bot_status, mem_info'},
        {}
    ),
)

UNIT_TEST_ARGS_2 = (
    (
        {'fqdns': 'test1.search'},
        {}
    ),
    (
        {'fqdns': 'test1.search,'},
        {}
    ),
    (
        {'fqdns': 'test1.search, test2.search'},
        {}
    ),
)

REAL_TEST_ARGS = (
    {'fqdns': 'dumdum.search.yandex.net', 'attr_names': 'bot_status'},
    {'fqdns': 'dumdum.search.yandex.net,', 'attr_names': 'bot_status'},
    {'fqdns': 'dumdum.search.yandex.net,dumdum2.search.yandex.net', 'attr_names': 'bot_status'},
    {'fqdns': 'dumdum.search.yandex.net', 'attr_names': 'bot_status,'},
    {'fqdns': 'dumdum.search.yandex.net', 'attr_names': 'bot_status,mem_info'},
    {'fqdns': 'dumdum.search.yandex.net,dumdum2.search.yandex.net', 'attr_names': 'bot_status,mem_info'},
)
