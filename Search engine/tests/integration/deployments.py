from search.mon.canti.back.src.models.deploy import DeployConfig, LocksConfig, DeploymentsQueue
from search.mon.canti.back.bin.config import load
# import inject

load()


def test_dep_queue():
    dq = DeploymentsQueue('//home/searchmon/dredd/canti/tests/')

    cfg1 = DeployConfig(
        responsibles=["mrt0rtikize"],
        need_confirm=False,
        locks=LocksConfig(
            path='tests',
            priority=0
        )
    )
    dq.add_deployment(
        flow_id='test1',
        tasklet_id='tset1',
        cfg=cfg1
    )

    cfg2 = DeployConfig(
        responsibles=["mrt0rtikize"],
        need_confirm=True,
        locks=LocksConfig(
            path='tests',
            priority=1
        )
    )
    dq.add_deployment(
        flow_id='test2',
        tasklet_id='tset2',
        cfg=cfg2
    )

    queue = dq.list_deploys()
    assert len(queue) == 2

    assert queue[0].info()['tasklet_id'] == 'tset2'
    assert queue[1].info()['tasklet_id'] == 'tset1'

    dq.process_queue()
    assert dq.get_deployment(flow_id='test2', tasklet_id='tset2').get_state()['state'] == 'waiting approve'
    assert dq.get_deployment(flow_id='test1', tasklet_id='tset1').get_state()['state'] == 'in queue'

    dq.confirm_deployment(flow_id='test2', tasklet_id='tset2', approver='qwe')
    dq.process_queue()
    assert dq.get_deployment(flow_id='test2', tasklet_id='tset2').get_state()['state'] == 'waiting approve'

    dq.confirm_deployment(flow_id='test2', tasklet_id='tset2', approver='mrt0rtikize')
    dq.process_queue()
    assert dq.get_deployment(flow_id='test2', tasklet_id='tset2').get_state()['state'] == 'in progress'

    dq.rm_deployment(flow_id='test2', tasklet_id='tset2')
    assert len(dq.list_deploys()) == 1
    dq.process_queue()
    assert dq.deployment_state(flow_id='test1', tasklet_id='tset1')['state'] == 'in progress'

    dq.rm_deployment(flow_id='test1', tasklet_id='tset1')

    assert len(dq.list_deploys()) == 0
