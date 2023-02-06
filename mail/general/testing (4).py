# coding: utf8
# kate: space-indent on; indent-width 4; replace-tabs on;
#
import os


WEB_TOOLS_VERSION = int(os.getenv("WEB_TOOLS_VERSION", "v2")[1:])
SSL_CONTEXT = None

SOSEARCH_HOST = os.getenv('SOSEARCH_HOST', "http://sosearch-proxy.mail.yandex.net:80")
SOSEARCH_TIMEOUT = int(os.getenv('SOSEARCH_TIMEOUT', 1))

SOLOGGER_HOST = os.getenv('SOLOGGER_HOST', "http://logger-testing.so.yandex-team.ru:80")
SOLOGGER_TIMEOUT = int(os.getenv('SOLOGGER_TIMEOUT', 1))

__all__ = [
    'WEB_TOOLS_VERSION', 'SSL_CONTEXT', 'SOSEARCH_HOST', 'SOSEARCH_TIMEOUT', 'SOLOGGER_HOST', 'SOLOGGER_TIMEOUT',
]

# ========== django_yauth

YAUTH_TYPE = 'desktop'

__all__ += ['YAUTH_TYPE', ]

# ========== TVM django-idm-api

IDM_TVM_CLIENT_ID = 2001602
IDM_INSTANCE = "testing"

__all__ += ['IDM_TVM_CLIENT_ID', 'IDM_INSTANCE', ]

# ========== DB settings

DB_PATH = os.getenv('DB_PATH')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'test_123')
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': DB_PATH,
        'MIGRATE': False,
    }
}

__all__ += ['DB_PATH', 'DATABASES', 'DB_PASSWORD', ]

# ==== Other settings

INSTALLED_APPS = [
#    'mail.so.spamstop.web.ui.web_tools',
    'rest_framework',
    "django_yauth",
    'django_idm_api',
    'django.contrib.sites',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'django_template_common',
    'mail.so.spamstop.web.ui.ut',
]

MIDDLEWARE = [
#    'django.middleware.security.SecurityMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django_yauth.middleware.YandexAuthTestMiddleware',
#    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
]

AUTHENTICATION_BACKENDS = [
    "django_yauth.authentication_mechanisms.dev.UserFromHttpHeaderAuthBackend",
    "django_yauth.authentication_mechanisms.dev.UserFromCookieAuthBackend",
    "django_yauth.authentication_mechanisms.dev.UserFromOsEnvAuthBackend",
]

__all__ += ['INSTALLED_APPS', 'MIDDLEWARE', 'AUTHENTICATION_BACKENDS', ]
