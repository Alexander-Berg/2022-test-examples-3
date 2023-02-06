from search.morty.proto.structures import component_pb2

from search.morty.src.model.nanny_predict import NannyPredict

from search.morty.tests.utils.test_case import MortyTestCase


class TestNannyPredict(MortyTestCase):
    def test_this(self):
        n = NannyPredict()
        component = component_pb2.Component(
            flows=component_pb2.FlowList(
                objects=[
                    component_pb2.Flow(
                        id='beta',
                        nanny=component_pb2.NannyFlow(
                            dashboard='uniproxy',
                            recipe='morty_uniproxy_beta',
                        ),
                    ),
                    component_pb2.Flow(
                        id='stable',
                        nanny=component_pb2.NannyFlow(
                            dashboard='uniproxy',
                            recipe='uniproxy-stable-deploy-morty',
                        ),
                    ),
                    component_pb2.Flow(
                        id='legacy',
                        nanny=component_pb2.NannyFlow(
                            dashboard='uniproxy',
                            recipe='uniproxy-legacy-only',
                        ),
                    ),
                ],
            ),
            component_name='uniproxy',
            parent_component_name='alice',
        )
        n.process_one(component)
        # create component and mock all
        # run once
        # check

        # run 2
        # check nothing
