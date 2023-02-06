import ujson
import uuid
import os

import yatest.common

from search.morty.proto.structures import event_pb2, component_pb2, recipe_pb2

from search.morty.src.model.process.alemate import NannyDeployRecipeGenerator, NannyDeployGenerator

from search.morty.tests.utils.test_case import MortyTestCase

# noinspection PyBroadException
try:
    data_path = yatest.common.source_path('search/morty/tests/test_data/test_nanny/recipes')
except:
    # Local debug mode
    data_path = f'/home/{os.getlogin()}/arcadia/search/morty/tests/test_data/test_nanny/recipes'

EVENT_BEGEMOT = event_pb2.Event(
    id=str(uuid.uuid4()),
    config=event_pb2.EventConfig(
        component_name='component',
        parent_component_name='parent',
        flow='flow',
    ),
    component=component_pb2.Component(
        component_name='component',
        parent_component_name='parent',
        flows=component_pb2.FlowList(
            objects=[
                component_pb2.Flow(
                    id='flow',
                    nanny=component_pb2.NannyFlow(
                        dashboard='begemot',
                        recipe='deploy',
                    )
                )
            ]
        )
    )
)


EVENT_WEB_FRONTEND = event_pb2.Event(
    id=str(uuid.uuid4()),
    config=event_pb2.EventConfig(
        component_name='component',
        parent_component_name='parent',
        flow='flow',
    ),
    component=component_pb2.Component(
        component_name='component',
        parent_component_name='parent',
        flows=component_pb2.FlowList(
            objects=[
                component_pb2.Flow(
                    id='flow',
                    nanny=component_pb2.NannyFlow(
                        dashboard='courier_web',
                        recipe='deploy_templates_without_shaverma',
                    )
                )
            ]
        )
    )
)


class TestNannyDeployRecipeGenerator(MortyTestCase):
    def test_generate(self):
        generator = NannyDeployRecipeGenerator()
        assert generator.generate(EVENT_BEGEMOT.component, EVENT_BEGEMOT.component.flows.objects[0])


class TestNannyDeployGenerator(MortyTestCase):
    def test_generate_begemot(self):
        generator = NannyDeployGenerator()

        with open(f'{data_path}/begemot_deploy.json') as fd:
            generator.service_crawler.clients.nanny.load_recipe(ujson.load(fd))

        subprocesses = generator.generate(EVENT_BEGEMOT.component, EVENT_BEGEMOT.component.flows.objects[0])

        recipe = generator.recipe_crawler.process_one(recipe_pb2.NannyRecipe(name='deploy', dashboard='begemot'))
        alemate_tasks = sorted(list(t.id for t in recipe.tasks))
        subprocesses_alemate_tasks = []
        for s in subprocesses:
            subprocesses_alemate_tasks.extend(s.tasks[0].params.control_alemate.alemate_tasks)
        assert sorted(subprocesses_alemate_tasks) == alemate_tasks

    def test_generate_web_frontend(self):
        generator = NannyDeployGenerator()

        with open(f'{data_path}/web-frontend.json') as fd:
            generator.service_crawler.clients.nanny.load_recipe(ujson.load(fd))

        subprocesses = generator.generate(EVENT_WEB_FRONTEND.component, EVENT_WEB_FRONTEND.component.flows.objects[0])

        recipe = generator.recipe_crawler.process_one(recipe_pb2.NannyRecipe(name='deploy_templates_without_shaverma', dashboard='courier_web'))
        alemate_tasks = sorted(list(t.id for t in recipe.tasks))
        subprocesses_alemate_tasks = []
        for s in subprocesses:
            subprocesses_alemate_tasks.extend(s.tasks[0].params.control_alemate.alemate_tasks)

        assert sorted(subprocesses_alemate_tasks) == alemate_tasks
