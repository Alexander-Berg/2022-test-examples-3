INSTALLED_APPS = [
    'django.contrib.sites',
    'travel.rasp.library.python.sitemap',
]

SECRET_KEY = 'test_secret_key'

TEMPLATES = [
    {
        'BACKEND': 'library.python.django.template.backends.arcadia.ArcadiaTemplates',
        'OPTIONS': {
            'debug': False,
            'loaders': [
                'library.python.django.template.loaders.app_resource.Loader',
            ],
        },
    }
]
