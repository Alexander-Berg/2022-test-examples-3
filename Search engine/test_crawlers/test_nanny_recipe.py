import ujson

import yatest.common
from google.protobuf.json_format import ParseDict

from search.morty.proto.structures import recipe_pb2

from search.morty.src.model.crawlers.nanny_recipe import CompressedGraph

from search.morty.tests.utils.test_case import MortyTestCase


data_path = yatest.common.source_path('search/morty/tests/test_data/test_nanny/recipes')


class TestCompressedGraph(MortyTestCase):
    def test_from_recipe(self):
        with open(f'{data_path}/begemot_deploy.json') as fd:
            recipe = ParseDict(ujson.load(fd)['content'], recipe_pb2.NannyRecipe(), ignore_unknown_fields=True)
        recipe_tasks = {t.id for t in recipe.tasks}

        graph = CompressedGraph(recipe)

        assert len(graph.nodes) == 4

        alemate_tasks = []
        for node in graph.nodes.values():
            alemate_tasks.extend(node.alemate_tasks)
        assert sorted(alemate_tasks) == sorted(recipe_tasks)
