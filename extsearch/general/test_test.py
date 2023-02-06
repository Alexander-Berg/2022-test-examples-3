from extsearch.video.robot.cm.library import test


class TestParseHostlist(object):

    def test_simple(self):
        hosts = test.parse_hostlist('hostlist_simple')
        assert hosts == {
            'MAIN': {'clusters': None},
        }

    def test_with_clusters(self):
        hosts = test.parse_hostlist('hostlist_with_clusters')
        assert hosts == {
            'MAIN': {
                'clusters': None,
            },
            'V3_EXFRAMES_RT': {
                'clusters': [
                    {'from': '000', 'to': '004'},
                ],
            },
            'V3_NNCALC_RT': {
                'clusters': [
                    {'from': '000', 'to': '039'},
                ],
            },
            'CONTENT': {
                'clusters': [
                    {'from': '0000', 'to': '0063'},
                    'vh_0000',
                ],
            },
        }

    def test_broken(self):
        try:
            hosts = test.parse_hostlist('hostlist_broken')
        except Exception as e:
            assert e.message == 'Duplicate name in hostlist: V3_EXFRAMES_RT'
        else:
            assert False, 'Expected exception'


class TestParseTargetTypes(object):

    def test_simple(self):
        target_types = test.parse_target_types('target_types_simple.scenario')
        assert target_types == {
            'MAIN': {'name': 'MAIN', 'clustered': False},
        }

    def test_with_clusters(self):
        target_types = test.parse_target_types('target_types_with_clusters.scenario')
        assert target_types == {
            'MAIN': {'name': 'MAIN', 'clustered': False},
            'V3_EXFRAMES_RT_W': {'name': 'V3_EXFRAMES_RT', 'clustered': True},
            'V3_NNCALC_RT_W': {'name': 'V3_NNCALC_RT', 'clustered': True},
            'CONTENT_SHARDS': {'name': 'CONTENT', 'clustered': True},
        }

    def test_broken(self):
        try:
            target_types = test.parse_target_types('target_types_broken.scenario')
        except Exception as e:
            assert e.message == 'Duplicate type in target types: V3_EXFRAMES_RT_W'
        else:
            assert False, 'Expected exception'


class TestParseTargets(object):

    def test_simple(self):
        targets = test.parse_targets('targets_simple.scenario')
        assert targets == {
            'url_state.start': {'type': 'MAIN', 'pos': 0},
            'url_state.merge': {'type': 'MAIN', 'pos': 1},
            'url_state.finish': {'type': 'MAIN', 'pos': 2},
        }

    def test_with_clusters(self):
        targets = test.parse_targets('targets_with_clusters.scenario')
        assert targets == {
            'common.push_usage': {'type': 'MAIN', 'pos': 0},
            'common.sigs.select_inputs': {'type': 'MAIN', 'pos': 1},
            'common.sigs.cleanup': {'type': 'MAIN', 'pos': 2},
            'v3.exframes.rt_workers': {'type': 'V3_EXFRAMES_RT_W', 'pos': 3},
            'v3.nncalc.rt_workers': {'type': 'V3_NNCALC_RT_W', 'pos': 4},
            'content.spread.make_content_portion': {'type': 'CONTENT_SHARDS', 'pos': 5},
            'content.spread.finish': {'type': 'MAIN', 'pos': 6},
        }

    def test_broken(self):
        try:
            targets = test.parse_targets('targets_broken.scenario')
        except Exception as e:
            assert e.message == 'Duplicate target in targets: v3.exframes.rt_workers'
        else:
            assert False, 'Expected exception'


class TestParseTargetsWithArgs(object):

    def test_simple(self):
        targets_with_args = test.parse_targets_with_args(
            'hostlist_simple',
            'target_types_simple.scenario',
            'targets_simple.scenario',
        )
        assert targets_with_args == [
            ['url_state.start'],
            ['url_state.merge'],
            ['url_state.finish'],
        ]

    def test_with_clusters(self):
        targets_with_args = test.parse_targets_with_args(
            'hostlist_with_clusters',
            'target_types_with_clusters.scenario',
            'targets_with_clusters.scenario',
        )
        assert targets_with_args == [
            ['common.push_usage'],
            ['common.sigs.select_inputs'],
            ['common.sigs.cleanup'],
            ['v3.exframes.rt_workers', '000'],
            ['v3.nncalc.rt_workers', '000'],
            ['content.spread.make_content_portion', '0000'],
            ['content.spread.finish'],
        ]
