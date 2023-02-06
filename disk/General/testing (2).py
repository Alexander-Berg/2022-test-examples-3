from .base import Settings as BaseSettings, LoggingSettings


class Settings(BaseSettings):
    logging: LoggingSettings = LoggingSettings()
    logging.file_enabled = True
    logging.stdout_json_formatted = True
