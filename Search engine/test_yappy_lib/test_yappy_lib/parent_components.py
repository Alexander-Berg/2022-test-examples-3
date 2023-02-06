from search.martylib.diff import index_by

from search.priemka.yappy.proto.structures.resources_pb2 import LocalSandboxFile
from search.priemka.yappy.proto.structures.sandbox_pb2 import SandboxFile
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent
from search.priemka.yappy.proto.structures.slot_pb2 import Slot


RESOURCE_MAP = {
    'external_id': [
        LocalSandboxFile(sandbox_file=SandboxFile(id='sandbox resource id')),
    ],
    'foobar': [
        LocalSandboxFile(sandbox_file=SandboxFile(id='foo'), local_path='foo.tar'),
        LocalSandboxFile(sandbox_file=SandboxFile(id='bar'), local_path='bar.txt'),
    ],
    'parent1': [
        LocalSandboxFile(local_path='foo.txt', sandbox_file=SandboxFile(
            id='FOO-1',
            task_type='BUILD_FOO',
            resource_type='FOO',
            task_id='1',
        )),
    ],
    'parent2': [
        LocalSandboxFile(local_path="instancectl", sandbox_file=SandboxFile(
            id="261125930",
            task_type="BUILD_INSTANCE_CTL",
            task_id="110553612",
            resource_type="INSTANCECTL",
        )),
        LocalSandboxFile(local_path="evlogdump", sandbox_file=SandboxFile(
            id="356977744",
            task_type="RELEASE_BEGEMOT_RESOURCES",
            task_id="154046204",
            resource_type="BEGEMOT_EVLOGDUMP",
        )),
        LocalSandboxFile(local_path="gdb_toolkit.tgz", sandbox_file=SandboxFile(
            id="357007474",
            task_type="RELEASE_BEGEMOT_RESOURCES",
            task_id="154046204",
            resource_type="GDB_SEARCH_TOOLKIT",
        )),
        LocalSandboxFile(local_path="begemot", sandbox_file=SandboxFile(
            id='351365812',
            task_type='RELEASE_BEGEMOT_RESOURCES',
            task_id='151518671',
            resource_type='BEGEMOT_EXECUTABLE',
        )),
    ],
    'parent3': [
        LocalSandboxFile(local_path="begemot", sandbox_file=SandboxFile(
            id='351365812',
            task_type='RELEASE_BEGEMOT_RESOURCES',
            task_id='151518671',
            resource_type='BEGEMOT_EXECUTABLE',
        )),
        LocalSandboxFile(local_path="gdb_toolkit.tgz", sandbox_file=SandboxFile(
            id="357007474",
            task_type="RELEASE_BEGEMOT_RESOURCES",
            task_id="154046204",
            resource_type="GDB_SEARCH_TOOLKIT",
        )),
        LocalSandboxFile(local_path="evlogdump", sandbox_file=SandboxFile(
            id="356977744",
            task_type="RELEASE_BEGEMOT_RESOURCES",
            task_id="154046204",
            resource_type="BEGEMOT_EVLOGDUMP",
        )),
        LocalSandboxFile(local_path="instancectl", sandbox_file=SandboxFile(
            id="261125930",
            task_type="BUILD_INSTANCE_CTL",
            task_id="110553612",
            resource_type="INSTANCECTL",
        )),
    ],
    'no_resources': [],
}

PARENT_COMPONENTS = []
for external_id, files in RESOURCE_MAP.items():
    bc = BetaComponent(
        id=external_id,
        do_not_manage=True,
        slot=Slot(type=Slot.Type.NANNY, id=external_id),
    )
    bc.current_state.resources.sandbox_files.extend(RESOURCE_MAP[external_id])
    PARENT_COMPONENTS.append(bc)

PARENTS_DATA = {}
for pbc in PARENT_COMPONENTS:
    PARENTS_DATA[pbc.slot.id] = index_by(
        pbc.current_state.resources.sandbox_files, 'local_path'
    )
