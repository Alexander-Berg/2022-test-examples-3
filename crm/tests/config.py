import environ
import datetime


def _list_converter(val):
    """Convert str to list of ints."""
    val_lst = val.strip().split(',')
    return [int(i) for i in val_lst]


@environ.config
class AppConfig:
    bunker_time_delta = environ.var(default=datetime.timedelta(0, 0, 0, 0, 0))
    max_count_of_intents_in_request = environ.var(name='MAX_COUNT_OF_INTENTS_IN_REQUEST', converter=int, default=5)
    oauth_token_direct = environ.var(name='OAUTH_TOKEN_DIRECT', converter=str, default='Bad oauth token!')
    environment = environ.var(name='YENV_TYPE', converter=str, default='testing')


app_config = environ.to_config(AppConfig)
