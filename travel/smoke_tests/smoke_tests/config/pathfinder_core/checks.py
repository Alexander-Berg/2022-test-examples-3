from travel.rasp.smoke_tests.smoke_tests.config.pathfinder_core.env import env
from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout},
        'urls': [
            [
                'search?from_type=settlement&from_id=2&to_type=station&to_id=9603013&date=2019-06-04+00%3A00%3A00'
                '&ttype=1&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=2&to_type=station&to_id=9603013&date={msk_now}'
                '&ttype=1&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=station&from_id=9612722&to_type=station&to_id=9612251&date={msk_today}'
                '&ttype=6&boarding=1440&max_delay=1380&can_change_in_any_town=1',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=54&to_type=station&to_id=9607511&date={msk_today}'
                '&ttype=6&boarding=1440&max_delay=1380&can_change_in_any_town=1',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=54&to_type=station&to_id=9607511&date={msk_today}'
                '&ttype=6&t_type=1&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=54&to_type=settlement&to_id=20691&date={msk_today}'
                '&ttype=6&boarding=1440&max_delay=1380&can_change_in_any_town=1',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=54&to_type=settlement&to_id=37&date={msk_today}'
                '&ttype=2&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=station&from_id=9600370&to_type=station&to_id=9627206&date={msk_today}'
                '&ttype=2&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=station&from_id=9600370&to_type=settlement&to_id=20&date={msk_today}'
                '&ttype=2&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],

            [
                'search?from_type=settlement&from_id=54&to_type=settlement&to_id=11&date={msk_today}'
                '&ttype=1&ttype=2&ttype=3&ttype=6&boarding=1440&max_delay=1380',
                {'stableness': StablenessVariants.TESTING_UNSTABLE}
            ],
        ],
    },
]
