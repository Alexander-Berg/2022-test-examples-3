from recommender_system import generate_recommender

from dj.lib.config_compiler.configs import Configs


def generate_recommender_test():
    recommender, cd = generate_recommender()

    return Configs(
        recommender=recommender,
        column_description=cd
    )
