import logging
from time import sleep

from tests_common.pytest_bdd import when

log = logging.getLogger(__name__)


def get_current_step_index(context):
    return next(i for i, step in enumerate(context.scenario.steps) if id(step) == id(context.step))


@when('repeat for "{times:d}"')
def when_repeat_step(context, times):
    return repeat_step_impl(**locals())


@when('repeat for "{times:d}" times with "{interval:g}" seconds interval')
def when_repeat_step_interval(context, times, interval):
    return repeat_step_impl(**locals())


def repeat_step_impl(context, times, interval=0):
    previous_step = context.scenario.steps[get_current_step_index(context) - 1]
    for _ in range(times):
        sleep(interval)
        step = '%s %s' % (previous_step.step_type, previous_step.name)
        log.info('Repeat step: %s' % step)
        context.execute_steps(step)
