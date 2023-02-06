from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants

himki = 'c10758'
moscow = 'c213'

ekb = 'c54'
tagil = 'c11168'

ping_url = ['ping']
urls = [
    [
        f'?pointFrom={ekb}&pointTo={tagil}&national_version=ru&minimalDistance=25',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        '?pointFrom=c54&pointTo=c56&national_version=ru',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        '?pointFrom=c54&pointTo=c56&date={msk_tomorrow}&national_version=ru',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        '?pointFrom=c54&pointTo=c56&date={msk_tomorrow}&national_version=ru&minimalDistance=75',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        '?pointFrom=c54&pointTo=c55&date={msk_tomorrow}&national_version=ru&isTouch=true',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        '?pointFrom=c54&pointTo=c55&date={msk_tomorrow}&national_version=ru&isTouch=false',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        'poll/?pointFrom=c54&pointTo=c56&national_version=ru',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
    [
        'poll/?pointFrom=c54&pointTo=c55&date={msk_tomorrow}&national_version=ru&isTouch=true',
        {'stableness': StablenessVariants.UNSTABLE}
    ],
]
urls_with_status_codes = [
    [
        '?pointFrom=c10745&pointTo=c21643&date={msk_today}&national_version=ru',
        {
            'code': 428
        }
    ],  # too far
    [
        f'?pointFrom={moscow}&pointTo={himki}&national_version=ru',
        {
            'code': 428
        }
    ],  # too close
    [
        '?pointFrom=c2&pointTo=c1&national_version=ru',
        {
            'code': 400,
            'stableness': StablenessVariants.UNSTABLE
        }
    ],  # c1 does not exists
]
