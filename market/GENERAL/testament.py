from datetime import timedelta

from library.python.monitoring.solo.objects.solomon.v2 import Type, Alert, Threshold, PredicateRule
from market.solo_monitorings.marketfront.registry.sensors.testament import make_platform_duration_sensor
from market.solo_monitorings.util.projects import market_front
from market.solo_monitorings.util.arcanum import generate_arcanum_dir_readme_url

PLATFORMS = [
    'touch',
    'desktop',
]

duration_alerts = [
    Alert(
        id=f'testament_{platform}_speed',
        name=f'0.75 перцентиль прохождения тестамен тестов в {platform}',
        project_id=market_front.id,
        description=f'''
            Тестамен тесты в CI стали проходить медленно.
            Для разбора читай readme: {generate_arcanum_dir_readme_url()}.
        ''',
        type=Type(
            threshold=Threshold(
                selectors=make_platform_duration_sensor(platform).selectors,
                time_aggregation="MAX",
                predicate="GT",
                threshold=60*20,
                predicate_rules=[
                    PredicateRule(
                        threshold_type="MAX",
                        comparison="GT",
                        threshold=60*20,
                        target_status="ALARM"
                    ),
                ]
            )
        ),
        window_secs=int(timedelta(minutes=30).total_seconds()),
    ) for platform in PLATFORMS]

exports = duration_alerts
