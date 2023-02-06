from library.python.monitoring.solo.objects.solomon.sensor import Sensor
from market.solo_monitorings.util.projects import market_front


# В секундах
def make_platform_duration_sensor(platform: str) -> Sensor:
    return Sensor(
        project=market_front.id, cluster="market-front_autotests_metrics", service="market-front_autotests_metrics",
        tag="tech_testament", quantile="0.75", sensor="duration", period="five_min", branch="feature",
        repo="market/marketfront", env="testing", type="unit", testsProject=f'white-{platform}',
    )
