class Stableness(object):
    STABLE = 'stable'
    UNSTABLE = 'unstable'


class StablenessConfig(object):
    RUN_STABLE = 'stable'
    RUN_UNSTABLE = 'unstable'
    RUN_ALL = 'all'


class TestStableness(object):
    def __init__(self, production, testing):
        # type: (str, str) -> None

        self.production = production
        self.testing = testing

    def is_runnable(self, env_name, env_stableness):
        # type: (str, str) -> bool

        config = self.production if env_name in {'production', 'prestable'} else self.testing

        if not env_stableness or env_stableness == StablenessConfig.RUN_ALL:
            return True

        if config == Stableness.STABLE and env_stableness == StablenessConfig.RUN_STABLE:
            return True

        if config == Stableness.UNSTABLE and env_stableness == StablenessConfig.RUN_UNSTABLE:
            return True

        return False


class StablenessVariants:
    STABLE = TestStableness(testing=Stableness.STABLE, production=Stableness.STABLE)
    TESTING_UNSTABLE = TestStableness(testing=Stableness.UNSTABLE, production=Stableness.STABLE)
    PRODUCTION_UNSTABLE = TestStableness(testing=Stableness.STABLE, production=Stableness.UNSTABLE)
    UNSTABLE = TestStableness(testing=Stableness.UNSTABLE, production=Stableness.UNSTABLE)
