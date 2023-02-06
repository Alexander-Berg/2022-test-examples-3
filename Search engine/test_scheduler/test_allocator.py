from search.morty.tests.utils.test_case import MortyTestCase

from search.morty.proto.structures import process_pb2, resource_pb2

from search.morty.src.common.const import INT_MAX
from search.morty.src.scheduler.allocator import AllocatedStorage, Allocator, TIME
from search.morty.src.scheduler.timeline import TimeInterval, LockTimeLine


class TestCommonStorage(MortyTestCase):
    def test_storage(self):
        storage = AllocatedStorage('test')
        assert storage.version == 'test'

        process = process_pb2.Process(id='process')
        subprocess = process_pb2.SubProcess(id='subprocess')

        storage[process] = 'process'
        storage[subprocess] = 'subprocess'
        try:
            storage['test'] = 'test'
            assert False
        except:
            pass

        assert storage[process] == 'process'
        assert storage.get(process) == 'process'
        assert storage[subprocess] == 'subprocess'
        assert storage.get(subprocess) == 'subprocess'

        storage.pop(process)
        storage.pop(subprocess)

        assert storage.get(process) is None
        assert storage.get(subprocess) is None


class TestAllocator(MortyTestCase):
    def test_init(self):
        allocator = Allocator([])
        assert not allocator._locks
        assert not allocator._allocated

        process = process_pb2.Process(
            subprocesses=[
                process_pb2.SubProcess(
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    verticals=('vertical', ),
                                    components=('component', ),
                                )
                            ]
                        ),
                        start=allocator.now - 10,
                        duration=5,
                    ),
                ),
                process_pb2.SubProcess(
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    verticals=('vertical_2',),
                                    components=('component_2',),
                                )
                            ]
                        ),
                        acquired=True,
                        start=allocator.now - 5,
                        duration=10,
                    ),
                ),
            ],
        )
        allocator = Allocator([process])
        assert set(allocator._locks.keys()) == {('vertical_2', '', 'component_2', '')}
        assert not allocator._allocated

    def test_schedule_subprocess(self):
        allocator = Allocator([])
        res = LockTimeLine()
        subprocess = process_pb2.SubProcess(
            id='subprocess',
            lock=resource_pb2.ResourceLock(
                resources=resource_pb2.ResourceList(
                    objects=[
                        resource_pb2.Resource(
                            verticals=('vertical', ),
                            components=('component', ),
                        )
                    ]
                ),
                duration=100,
            ),
        )
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        res._timeline = {TimeInterval(0, INT_MAX - 100)}
        assert scheduled[subprocess] == res

        allocator._locks[('vertical', '', '', '')].add(interval=TimeInterval(150, 300))
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        res._timeline = {TimeInterval(0, 45), TimeInterval(305, INT_MAX - 100)}
        assert scheduled[subprocess] == res

        allocator._locks[('', '', 'component', '')].add(interval=TimeInterval(350, 400))
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        res._timeline = {TimeInterval(0, 45), TimeInterval(405, INT_MAX - 100)}
        assert scheduled[subprocess] == res

        allocator._locks[('vertical', '', '', '')].add(interval=TimeInterval(350, 400))
        allocator._locks[('', '', 'component', '')].add(interval=TimeInterval(150, 300))
        allocator._locks[('vertical', '', 'component', '')].add(interval=TimeInterval(150, 300))
        allocator._locks[('vertical', '', 'component', '')].add(interval=TimeInterval(350, 400))
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        assert scheduled[subprocess] == res

        subprocess.lock.acquired = True
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        res._timeline = {TimeInterval(0, 0)}
        assert scheduled[subprocess] == res

        subprocess.lock.start = 200
        subprocess.lock.acquired = False
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        res._timeline = {TimeInterval(0, 45), TimeInterval(405, INT_MAX - 100)}
        assert scheduled[subprocess] == res

        subprocess.lock.start = allocator.now + 100
        res._timeline = {TimeInterval(0, 45), TimeInterval(405, INT_MAX - 100)}
        scheduled = allocator._Allocator__schedule_subprocess(subprocess, TIME)
        assert scheduled[subprocess] == res

    def test_schedule_process(self):
        allocator = Allocator([])
        process = process_pb2.Process(
            id='process',
            subprocesses=[
                process_pb2.SubProcess(
                    id='subprocess_1',
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    verticals=('vertical',),
                                    components=('component',),
                                ),
                            ],
                        ),
                        duration=100,
                    ),
                ),
                process_pb2.SubProcess(
                    id='subprocess_2',
                    required=['subprocess_1'],
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    endpoints=('endpoint', ),
                                ),
                            ],
                        ),
                        duration=200,
                    ),
                ),
            ],
        )

        scheduled = allocator.schedule(process)
        assert scheduled[process.subprocesses[0]] == LockTimeLine(0, INT_MAX - 100)
        assert scheduled[process.subprocesses[1]] == LockTimeLine(0, INT_MAX - 200)

    def test_allocate(self):
        allocator = Allocator([])
        process = process_pb2.Process(
            id='process',
            subprocesses=[
                process_pb2.SubProcess(
                    id='subprocess',
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    endpoints=('endpoint',),
                                ),
                            ],
                        ),
                        duration=200,
                    ),
                ),
                process_pb2.SubProcess(
                    id='subprocess2',
                    required=['subprocess'],
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    verticals=('vertical',)
                                ),
                            ],
                        ),
                        duration=500,
                    ),
                ),
            ]
        )
        allocated = allocator.allocate(process)
        assert allocated[process].start == 0
        assert allocated[process.subprocesses[0]].start == 0
        assert allocated[process.subprocesses[1]].start == 205

        # test fail
        # test link: o2o m2o o2m m2m

    def test_lock(self):
        allocator = Allocator([])
        process = process_pb2.Process(
            id='process',
            subprocesses=[
                process_pb2.SubProcess(
                    id='subprocess',
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    endpoints=('endpoint',),
                                ),
                            ],
                        ),
                        duration=200,
                    ),
                ),
                process_pb2.SubProcess(
                    id='subprocess2',
                    required=['subprocess'],
                    lock=resource_pb2.ResourceLock(
                        resources=resource_pb2.ResourceList(
                            objects=[
                                resource_pb2.Resource(
                                    verticals=('vertical',)
                                ),
                            ],
                        ),
                        duration=500,
                    ),
                ),
            ]
        )
        allocator.allocate(process)
        allocator.lock(process)

        assert process.lock.start == 0
        assert process.subprocesses[0].lock.start == 0
        assert process.subprocesses[1].lock.start == 205

        assert allocator._locks[('', '', '', 'endpoint')] == LockTimeLine(0, 200)
        assert allocator._locks[('vertical', '', '', '')] == LockTimeLine(205, 705)
