import json

from library.python.testing.recipe import declare_recipe, set_env


def start(argv):
    with open('pg_recipe.json', 'r') as pg_config_file:
        pg_config = json.load(pg_config_file)
        set_env("DB_URL", 'postgresql+psycopg2://{user}@:{port}/{dbname}?host={host}'.format(**pg_config))


def stop(argv):
    pass


if __name__ == "__main__":
    declare_recipe(start, stop)
