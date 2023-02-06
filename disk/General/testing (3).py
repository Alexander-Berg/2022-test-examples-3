from .base import Settings as BaseSettings


class Settings(BaseSettings):
    dns_master_url: str = "http://dns-master.test.ws.yandex.net"
    init_stuck_zones_count: int = 5
    init_sleep_time: int = 30
