from tvmauth import BlackboxTvmId

from .base import Settings as BaseSettings, LoggingSettings


class Settings(BaseSettings):
    blackbox_client = BlackboxTvmId.Test

    logging: LoggingSettings = LoggingSettings()
    logging.file_path = "/app/log/dns_cron.log"
    logging.file_enabled = True
    logging.stdout_json_formatted = True
