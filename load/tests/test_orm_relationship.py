from load.projects.cloud.loadtesting.db.tables import job, job_config, ammo, tank, operation


def test_config_job_relationship():
    test_job = job.JobTable(id='job 1')
    test_config = job_config.JobConfigTable(id='config 1')
    test_job.config = test_config
    assert test_job.config.id == 'config 1'
    assert test_config.job.id == 'job 1'


def test_ammo_job_relationship():
    test_job_1 = job.JobTable(id='job 1')
    test_ammo_1 = ammo.AmmoTable(id='ammo 1')
    test_ammo_2 = ammo.AmmoTable(id='ammo 2')
    test_job_1.ammos.append(test_ammo_1)
    test_job_1.ammos.append(test_ammo_2)
    assert len(test_job_1.ammos) == 2
    assert test_job_1.ammos[0].id == 'ammo 1'
    assert len(test_ammo_1.jobs) == 1
    assert test_ammo_1.jobs[0].id == 'job 1'


def test_tank_job_relationship():
    test_tank = tank.TankTable(id='tank 1')
    test_job_1 = job.JobTable(id='job 1')
    test_job_2 = job.JobTable(id='job 2')
    test_job_1.tank = test_tank
    test_job_2.tank = test_tank
    assert len(test_tank.jobs) == 2
    assert test_job_1.tank.id == 'tank 1'
    assert test_tank.jobs[1].id == 'job 2'


def test_operations_relationship():
    test_tank = tank.TankTable(id='tank 1')
    test_job = job.JobTable(id='job 1')
    test_op_1 = operation.OperationTable(id='operation 1')
    test_op_1.tank = test_tank
    assert test_op_1.tank.id == 'tank 1'
    test_op_2 = operation.OperationTable(id='operation 2')
    test_op_2.job = test_job
    assert test_op_2.job.id == 'job 1'
