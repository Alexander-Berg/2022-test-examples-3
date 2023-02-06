# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.config.suggests.env import env


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout},
        'urls': [
            'ping',
            'all_suggests?client_city=213&field=to&format=old&lang=ru&national_version=ru&other_point=s9600741&part=новопетр',
            'by_t_type?client_city=14&field=to&format=new&lang=ru&national_version=ru&other_point=c14&part=&t_type_code=train',
        ],
    },
]
