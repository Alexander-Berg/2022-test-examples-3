import setuptools


configuration = {
    "name": "rta",
    "version": "0.0.2",
    "description": "Yandex Market Report testing assistant",
    "url": "",
    "author": "Iskander Sitdikov",
    "author_email": "thoughteer@yandex-team.ru",
    "packages": [
        "rta",
    ],
    "dependency_links": [
        "https://github.yandex-team.ru/tools/startrek-python-client/archive/1.2.9.tar.gz#egg=startrek_client-1.2.9",
    ],
    "install_requires": [
        "bottle >= 0.11",
        "jinja2 >= 2.2.8",
        "startrek_client >= 1.2.2",
    ],
    "package_data": {
        "rta": [
            "data/rta.cfg",
            "data/templates/*.html",
            "data/static/css/*.css",
            "data/static/html/*.html",
            "data/static/png/*.png",
        ]
    },
    "zip_safe": False,
}
setuptools.setup(**configuration)
