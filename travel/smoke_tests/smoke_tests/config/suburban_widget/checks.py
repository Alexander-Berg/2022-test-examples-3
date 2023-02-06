from travel.rasp.smoke_tests.smoke_tests.config.suburban_widget.env import env


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout},
        'urls': [
            'ping',
            'widgets/suburban/next_trains/settings?frommorda=1&&rasp_city=&rasp_direction=&rasp_from=&rasp_to=&minutes_forward=&minutes_backward=&n=&geo=10740',
            'widgets/suburban/next_trains?rasp_to=9633753&rasp_direction=33&rasp_from=9602496&n=5',
            ['widgets/suburban/directions/?zone=3', {'timeout': env.timeout_slow}],
            'widgets/suburban/stations?zone=3&dirtection=31',
            'jsi18n/',
        ],
    },
]
