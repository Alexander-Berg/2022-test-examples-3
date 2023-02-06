
def mypy_check_root() -> str:
    return 'travel/avia/ad_feed'


def mypy_config_resource() -> tuple[str, str]:
    return '__tests__', 'travel/avia/ad_feed/mypy.ini'
