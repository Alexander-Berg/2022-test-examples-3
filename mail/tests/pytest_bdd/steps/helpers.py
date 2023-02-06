from tests_common.pytest_bdd import then, when


def then_step(step_desc, func):
    @then(step_desc)
    def deco_func(*args, **kwargs):
        return func(*args, **kwargs)

    return deco_func


def when_step(step_desc, func):
    @when(step_desc)
    def deco_func(*args, **kwargs):
        return func(*args, **kwargs)

    return deco_func
