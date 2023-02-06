# coding: utf-8

import uuid
from sqlalchemy import column

from search.martylib.core.date_utils import mock_now
from search.martylib.core.exceptions import NotAuthorized
from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.martylib.proto.structures.yp_lite_pb2 import AllocationRequest

from search.priemka.yappy.proto.structures.api_pb2 import (
    ApiBetaComponent,
    ApiBetaFilter,
    ApiBeta,
    ApiCheck,
    ApiComponentType,
    ApiQuota,
    CreateBetaFromConfig,
    CreateBetaFromTemplate,
    UpdatePatches,
)
from search.priemka.yappy.proto.structures.balancer_pb2 import BalancerSpec
from search.priemka.yappy.proto.structures.beta_component_pb2 import Tags
from search.priemka.yappy.proto.structures.bolver_pb2 import (
    BolverSpec,
    BolverSpecList,
    SingleComponentBetaBolverSpec,
    ComponentBolverSpec,
)
from search.priemka.yappy.proto.structures.nanny_pb2 import NannySpec, NannySpecPatch, YpPodsSpec
from search.priemka.yappy.proto.structures.patch_pb2 import Patch
from search.priemka.yappy.proto.structures.payload_pb2 import (
    Lineage2Response,
    Lineage2ValidationError,
)
from search.priemka.yappy.proto.structures.resources_pb2 import InstanceSpec
from search.priemka.yappy.proto.structures.slot_pb2 import Slot
from search.priemka.yappy.proto.structures.yp_pb2 import YP
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.src.yappy_lib.utils import enum_to_string, get_beta_name_from_template
from search.priemka.yappy.src.model.model_service.workers.allocator import Allocator

from search.priemka.yappy.tests.utils.test_cases import LineageIITestCase


class CreateBeta(LineageIITestCase):
    def create_test_data(self):
        default_component_type = model.ComponentType(name='default_component_type')
        default_quota = model.Quota(name='default_quota')
        default_slots = [
            model.Slot(id='default_slot_1', type=enum_to_string(Slot.Type, Slot.Type.NANNY), quota_name=default_quota.name),
            model.Slot(id='default_slot_2', type=enum_to_string(Slot.Type, Slot.Type.NANNY))
        ]
        default_components = [
            model.BetaComponent(id=uuid.uuid4(), type=default_component_type, quota=default_quota),
            model.BetaComponent(id=uuid.uuid4(), type=default_component_type, slot=default_slots[1])
        ]

        default_beta = model.Beta(name='default_beta', components=default_components)

        with session_scope() as session:
            session.merge(default_beta)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_beta(self):
        request = ApiBeta(
            name='test-beta',
            components=[
                ApiBetaComponent(
                    type='default_component_type',
                    quota='default_quota',
                    do_not_manage=True,
                ),
            ]
        )

        with self.mock_request() as ctx:
            resp = self.lineage2.create_beta(request, ctx)
            self.assertEqual(resp.status, Lineage2Response.Status.SUCCESS)


class CreateBetaFromTemplateTest(LineageIITestCase):
    def create_test_data(self):
        default_component_type = model.ComponentType(name='default_component_type')
        default_quota = model.Quota(
            name='default_quota',
        )
        default_quota.slots.append(model.Slot(id='slot-1'))
        default_beta_component_templates = [
            model.BetaComponentTemplate(
                id='default_bct_1',
                type=default_component_type,
                quota=default_quota
            ),
        ]
        default_beta_template = model.BetaTemplate(
            name='default-beta-template',
            components=default_beta_component_templates,
            ttl=72,
        )

        with session_scope() as session:
            session.merge(default_beta_template)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_beta_from_template(self):
        request = CreateBetaFromTemplate(
            template_name='default-beta-template',
            suffix='test',
            patches=[
                CreateBetaFromTemplate.PatchMap(
                    component_id='default_bct_1',
                    patch=Patch(
                        parent_external_id='test-parent',
                    )
                )
            ]
        )

        with self.mock_request() as ctx:
            resp = self.lineage2.create_beta_from_beta_template(request, ctx)
            self.assertEqual(resp.status, Lineage2Response.Status.SUCCESS)

        with session_scope() as session:
            beta = session.query(model.Beta).filter(model.Beta.name == get_beta_name_from_template(request.template_name, request.suffix)).first()

            self.assertNotEquals(beta, None)
            self.assertEqual(len(beta.components), 1)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_beta_expiration(self):
        request = CreateBetaFromTemplate(
            update_if_exist=True,
            template_name='default-beta-template',
            suffix='test-1',
            patches=[
                CreateBetaFromTemplate.PatchMap(
                    component_id='default_bct_1',
                    patch=Patch(parent_external_id='test-parent'),
                )
            ]
        )
        beta_name = get_beta_name_from_template(request.template_name, request.suffix)

        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_beta_template(request, ctx)
            self.lineage2.allocate_beta(ApiBetaFilter(name=beta_name), ctx)

        with session_scope() as session:
            expires = session.query(model.Beta.expires).filter(model.Beta.name == beta_name).scalar()

        self.assertNotEqual(expires, 0)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_updated_beta_expiration(self):
        request = CreateBetaFromTemplate(
            update_if_exist=True,
            template_name='default-beta-template',
            suffix='test-2',
            patches=[
                CreateBetaFromTemplate.PatchMap(
                    component_id='default_bct_1',
                    patch=Patch(parent_external_id='test-parent'),
                )
            ]
        )
        beta_name = get_beta_name_from_template(request.template_name, request.suffix)

        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_beta_template(request, ctx)
            self.lineage2.allocate_beta(ApiBetaFilter(name=beta_name), ctx)
            self.lineage2.create_beta_from_beta_template(request, ctx)

        with session_scope() as session:
            expires = session.query(model.Beta.expires).filter(model.Beta.name == beta_name).scalar()

        self.assertNotEqual(expires, 0)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_updated_beta_ttl_expiration(self):
        request = CreateBetaFromTemplate(
            update_if_exist=True,
            template_name='default-beta-template',
            suffix='test-3',
            patches=[
                CreateBetaFromTemplate.PatchMap(
                    component_id='default_bct_1',
                    patch=Patch(parent_external_id='test-parent'),
                )
            ]
        )
        beta_name = get_beta_name_from_template(request.template_name, request.suffix)
        now_ = 10000

        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_beta_template(request, ctx)
            self.lineage2.allocate_beta(ApiBetaFilter(name=beta_name), ctx)
            with mock_now(now_):
                self.lineage2.create_beta_from_beta_template(request, ctx)

        with session_scope() as session:
            expires = session.query(model.Beta.expires).filter(model.Beta.name == beta_name).scalar()

        expiration_without_ttl = Allocator.get_beta_expire_time(0, now_)
        self.assertNotEqual(expires, expiration_without_ttl)


class UpdatePatchesTest(LineageIITestCase):
    def create_test_data(self):
        default_component_type_0 = model.ComponentType(name='default_component_type_0')
        default_component_type_1 = model.ComponentType(name='default_component_type_1')
        default_component_0 = model.BetaComponent(id=uuid.uuid4(), type=default_component_type_0, patch=Patch(expected_checkconfig=['123']).SerializeToString(), template_id='1')
        default_component_1 = model.BetaComponent(id=uuid.uuid4(), type=default_component_type_1, patch=Patch(expected_checkconfig=['qwer']).SerializeToString(), template_id='2')
        default_beta = model.Beta(name='default_beta', components=[default_component_0, default_component_1])

        with session_scope() as session:
            session.add(default_beta)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_update_patches_template_id(self):
        request = UpdatePatches(
            beta_name='default_beta',
            patches=[
                UpdatePatches.PatchMap(
                    template_id='1',
                    patch=Patch(expected_checkconfig=['new patch 1'])
                ),
                UpdatePatches.PatchMap(
                    template_id='2',
                    patch=Patch(expected_checkconfig=['new patch 2'])
                ),
            ]
        )

        with self.mock_request() as ctx:
            resp = self.lineage2.update_patches(request, ctx)
            self.assertEqual(resp.status, Lineage2Response.Status.SUCCESS)

        with session_scope() as session:
            beta = session.query(model.Beta).filter(model.Beta.name == 'default_beta').first()

            for component in beta.components:   # type: model.BetaComponent
                patch = Patch()
                patch.MergeFromString(component.patch)
                self.logger.info(patch)

                if component.yappy__ComponentType_name == 'default_component_type_0':
                    self.assertEqual(patch.expected_checkconfig, ['new patch 1'])
                else:
                    self.assertEqual(patch.expected_checkconfig, ['new patch 2'])

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_update_patches_component_type(self):
        request = UpdatePatches(
            beta_name='default_beta',
            patches=[
                UpdatePatches.PatchMap(
                    type='default_component_type_0',
                    patch=Patch(expected_checkconfig=['new patch 1'])
                ),
                UpdatePatches.PatchMap(
                    type='default_component_type_1',
                    patch=Patch(expected_checkconfig=['new patch 2'])
                ),
            ]
        )

        with self.mock_request() as ctx:
            resp = self.lineage2.update_patches(request, ctx)
            self.assertEqual(resp.status, Lineage2Response.Status.SUCCESS)

        with session_scope() as session:
            beta = session.query(model.Beta).filter(model.Beta.name == 'default_beta').first()

            for component in beta.components:   # type: model.BetaComponent
                patch = Patch()
                patch.MergeFromString(component.patch)
                self.logger.info(patch)

                if component.yappy__ComponentType_name == 'default_component_type_0':
                    self.assertEqual(patch.expected_checkconfig, ['new patch 1'])
                else:
                    self.assertEqual(patch.expected_checkconfig, ['new patch 2'])


class QuotaTest(LineageIITestCase):
    def create_test_data(self):
        with session_scope() as session:
            session.add(model.Quota(name='existing-quota'))

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_quota_already_exists(self):
        request = ApiQuota(name='existing-quota')
        with self.mock_request() as ctx:
            result = self.lineage2.create_quota(request, ctx)
            result = Lineage2Response(status=result.status, validation_errors=result.validation_errors)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=[Lineage2ValidationError(msg='quota {} is already exist'.format(request.name))],
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_quota(self):
        request = ApiQuota(name='new-test-quota')
        with self.mock_request() as ctx:
            self.lineage2.create_quota(request, ctx)
        with session_scope() as session:
            result = session.query(model.Quota.name).filter(model.Quota.name == request.name).scalar()
        expected = request.name
        self.assertEqual(result, expected)


class ComponentTypeTest(LineageIITestCase):
    def create_test_data(self):
        with session_scope() as session:
            session.add(model.ComponentType(name='existing-type'))

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_type_already_exists(self):
        request = ApiComponentType(name='existing-type')
        with self.mock_request() as ctx:
            result = self.lineage2.create_component_type(request, ctx)
            result = Lineage2Response(status=result.status, validation_errors=result.validation_errors)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=[Lineage2ValidationError(msg='component type {} is already exist'.format(request.name))],
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user', roles=['yappy/admin'])
    def test_create_type(self):
        request = ApiComponentType(name='new-test-type')
        with self.mock_request() as ctx:
            self.lineage2.create_component_type(request, ctx)
        with session_scope() as session:
            result = session.query(model.ComponentType.name).filter(model.ComponentType.name == request.name).scalar()
        expected = request.name
        self.assertEqual(result, expected)


class CreateBetaFromConfigTest(LineageIITestCase):
    @classmethod
    def setUpClass(cls):
        super(CreateBetaFromConfigTest, cls).setUpClass()
        cls.minimal_config = CreateBetaFromConfig(
            name="beta-name",
            nanny_spec=NannySpec(
                copy_from='template-nanny-service',
                yp_spec=YpPodsSpec(
                    abc_service_id=123,
                    pod_spec=AllocationRequest(
                        replicas=2,
                    )
                )
            )
        )
        cls.full_config = CreateBetaFromConfig(
            name="beta-name",
            disable_default_checks=True,
            consistency_checks=[
                ApiCheck(check_class='beta.base.CheckOne'),
                ApiCheck(check_class='base.CheckTwo'),
                ApiCheck(check_class='beta.some.CheckThree'),
            ],
            nanny_spec=NannySpec(
                copy_from='template-nanny-service',
                add_resources_from='resources-template-nanny-service',
                copy_coredump_policy=True,
                translate_auth_attrs=True,
                translate_secrets=True,
                patch=NannySpecPatch(
                    resources=[
                        Patch.Resource(
                            manage_type=Patch.Resource.ManageType.STATIC_CONTENT,
                            local_path='file-with-static-content',
                            content='some static content',
                        ),
                    ],
                    instance_spec=InstanceSpec(
                        type=InstanceSpec.InstanceType.SANDBOX_LAYERS,
                    ),
                ),
                yp_spec=YpPodsSpec(
                    abc_service_id=123,
                    use_tmp_quota=True,
                    pod_spec=AllocationRequest(
                        replicas=2,
                    ),
                    clusters=[YP.ClusterType.SAS],
                ),
                instance_tags=Tags(
                    ctype='ctype-tag',
                    itype='itype-tag',
                    metaprj='metaprj-tag',
                    prj='prj-tag',
                    tier='tier-tag',
                ),
            ),
            balancer_spec=BalancerSpec(
                host='balancer.fqdn',
                bolver=SingleComponentBetaBolverSpec(
                    use_internal_flags=True,
                    do_not_push=True,
                    no_meta_cgi=True,
                    rps_limiter_quota='limiter-quota-name',
                    json={'key': 'value'},
                    specs=[
                        ComponentBolverSpec(
                            type=BolverSpec.Type.BALANCER,
                            port_offset=50,
                        ),
                        ComponentBolverSpec(
                            type=BolverSpec.Type.NOAPACHE,
                            collection='collection',
                        ),
                    ],
                ),
            ),
        )

    def create_test_data(self):
        with session_scope() as session:
            session.add(model.Beta(name='existing-beta'))

    @staticmethod
    def required_field_errors(*fields):
        return [
            Lineage2ValidationError(
                field=field,
                msg='field is required',
            )
            for field in fields
        ]

    def test_not_authorized(self):
        with self.mock_request() as ctx:
            self.assertRaises(
                NotAuthorized,
                self.lineage2.create_beta_from_config,
                CreateBetaFromConfig(),
                ctx,
            )

    @TestCase.mock_auth(login='test-user')
    def test_create_already_exists(self):
        request = CreateBetaFromConfig()
        request.CopyFrom(self.full_config)
        request.name = 'existing-beta'
        with self.mock_request() as ctx:
            result = self.lineage2.create_beta_from_config(request, ctx)
            result = Lineage2Response(status=result.status, validation_errors=result.validation_errors)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=[Lineage2ValidationError(msg='beta {} is already exist'.format(request.name))]
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_empty_config(self):
        request = CreateBetaFromConfig()
        with self.mock_request() as ctx:
            result = self.lineage2.create_beta_from_config(request, ctx)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=self.required_field_errors('name', 'nanny_spec'),
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_name_only(self):
        request = CreateBetaFromConfig(name="beta-name")
        with self.mock_request() as ctx:
            result = self.lineage2.create_beta_from_config(request, ctx)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=self.required_field_errors('nanny_spec'),
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_incomplete_nanny_spec(self):
        request = CreateBetaFromConfig(name="beta-name", nanny_spec=NannySpec(translate_secrets=True))
        with self.mock_request() as ctx:
            result = self.lineage2.create_beta_from_config(request, ctx)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=self.required_field_errors('nanny_spec.copy_from', 'nanny_spec.yp_spec'),
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_incomplete_yp_spec(self):
        request = CreateBetaFromConfig(
            name="beta-name",
            nanny_spec=NannySpec(
                copy_from='template-nanny-service',
                yp_spec=YpPodsSpec(use_tmp_quota=True),
            )
        )
        with self.mock_request() as ctx:
            result = self.lineage2.create_beta_from_config(request, ctx)
        expected = Lineage2Response(
            status=Lineage2Response.Status.FAILED,
            validation_errors=self.required_field_errors(
                'nanny_spec.yp_spec.abc_service_id',
                'nanny_spec.yp_spec.pod_spec'
            ),
        )
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_minimal_config_response(self):
        request = self.minimal_config
        with self.mock_request() as ctx:
            response = self.lineage2.create_beta_from_config(request, ctx)

        result = response.status
        expected = Lineage2Response.Status.SUCCESS

        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_minimal_config_db_records_created(self):
        request = self.minimal_config
        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)

        with session_scope() as session:
            data = (
                session.query(
                    model.Beta.name,
                    model.BetaComponent.id,
                    model.Quota.name,
                    model.ComponentType.name,
                )
                .join(model.Beta.components)
                .join(model.Quota)
                .join(model.ComponentType)
                .filter(model.Beta.name == request.name)
                .first()
            )
            objects = ['beta', 'component', 'quota', 'type']
            result = dict(zip(objects, [bool(name) for name in data]))

        expected = {obj: True for obj in objects}

        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_full_config_beta_data(self):
        request = self.full_config
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            beta_data = (
                session.query(
                    model.Beta.name,
                    model.Beta.last_update,
                    model.Beta.bolver_spec,
                    model.Beta.balancer_host,
                )
                .filter(model.Beta.name == request.name)
                .first()
            )

            result = beta_data._asdict()

            bolver_spec = BolverSpec()
            bolver_spec.MergeFromString(result['bolver_spec'])
            result['bolver_spec'] = bolver_spec

        expected = {
            'name': request.name,
            'last_update': 123,
            'bolver_spec': BolverSpec(
                use_internal_flags=request.balancer_spec.bolver.use_internal_flags,
                do_not_push=request.balancer_spec.bolver.do_not_push,
                no_meta_cgi=request.balancer_spec.bolver.no_meta_cgi,
                rps_limiter_quota=request.balancer_spec.bolver.rps_limiter_quota,
                json=request.balancer_spec.bolver.json,
            ),
            'balancer_host': request.balancer_spec.host,
        }
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_full_config_component_type_data(self):
        request = self.full_config
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            type_data = (
                session.query(
                    model.ComponentType.last_update,
                    model.ComponentType.auto_created,
                    model.ComponentType.bolver_specs,
                    model.ComponentType.yp,
                )
                .join(model.Beta.components)
                .filter(model.Beta.name == request.name)
                .first()
            )
            result = type_data._asdict()

            yp_proto = YP()
            yp_proto.MergeFromString(result['yp'])
            result['yp'] = yp_proto

            bolver_specs = BolverSpecList()
            bolver_specs.MergeFromString(result['bolver_specs'])
            result['bolver_specs'] = bolver_specs

        expected = {
            'last_update': 123,
            'auto_created': True,
            'bolver_specs': BolverSpecList(
                objects=[
                    BolverSpec(
                        type=BolverSpec.Type[spec.type],
                        port_offset=spec.port_offset,
                        additional_params=spec.additional_params,
                        source_names=spec.source_names,
                        headers=spec.headers,
                        use_header_templates=spec.use_header_templates,
                        collection=spec.collection,
                        timeout=spec.timeout,
                        shorten_fqdns=spec.shorten_fqdns,
                    )
                    for spec in request.balancer_spec.bolver.specs
                ],
            ),
            'yp': YP(
                service_template_id=request.nanny_spec.copy_from,
                pod_spec=request.nanny_spec.yp_spec.pod_spec,
                clusters=request.nanny_spec.yp_spec.clusters,
            ),
        }
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_create_with_full_config_quota_data(self):
        request = self.full_config
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            quota_data = (
                session.query(
                    model.Quota.last_update,
                    model.Quota.auto_created,
                    model.Quota.abc_service_id,
                    model.Quota.tmp_quota,
                )
                .join(model.Beta.components)
                .filter(model.Beta.name == request.name)
                .first()
            )
            result = quota_data._asdict()
        expected = {
            'last_update': 123,
            'auto_created': True,
            'abc_service_id': request.nanny_spec.yp_spec.abc_service_id,
            'tmp_quota': request.nanny_spec.yp_spec.use_tmp_quota,
        }
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_resources_patch_template_service(self):
        request = self.full_config
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            patch_data = (
                session.query(model.BetaComponent.patch)
                    .join(model.yappy__Beta__components)
                    .filter(column('yappy__Beta_name') == request.name)
                    .scalar()
            )
            result = Patch.FromString(patch_data).parent_external_id

        expected = request.nanny_spec.add_resources_from
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_resources_patch_template_service_not_specified(self):
        request = CreateBetaFromConfig()
        request.CopyFrom(self.full_config)
        request.nanny_spec.ClearField('add_resources_from')
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            patch_data = (
                session.query(model.BetaComponent.patch)
                .join(model.yappy__Beta__components)
                .filter(column('yappy__Beta_name') == request.name)
                .scalar()
            )
            result = Patch.FromString(patch_data).parent_external_id

        expected = request.nanny_spec.copy_from
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_patch_coredump_policy_flag(self):
        request = self.full_config
        with mock_now(123), self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            patch_data = (
                session.query(model.BetaComponent.patch)
                    .join(model.yappy__Beta__components)
                    .filter(column('yappy__Beta_name') == request.name)
                    .scalar()
            )
            result = Patch.FromString(patch_data).copy_coredump_policy

        expected = request.nanny_spec.copy_coredump_policy
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login='test-user')
    def test_instance_tags(self):
        request = self.full_config
        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            tags_data = (
                session.query(model.BetaComponent.tags)
                    .join(model.yappy__Beta__components)
                    .filter(column('yappy__Beta_name') == request.name)
                    .scalar()
            )
            tags = Tags.FromString(tags_data)

        expected = request.nanny_spec.instance_tags
        self.assertEqual(tags, expected)

    @TestCase.mock_auth(login='test-user')
    def test_beta_checks(self):
        request = CreateBetaFromConfig()
        request.MergeFrom(self.full_config)
        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            beta_checks = (
                session.query(model.Check.check_class)
                .filter(column('yappy__Beta_name') == request.name)
                .all()
            )
        beta_check_prefix = 'beta.'
        expected = sorted([
            check.check_class[len(beta_check_prefix):]
            for check in self.full_config.consistency_checks
            if check.check_class.startswith(beta_check_prefix)
        ])
        beta_checks = sorted([check.check_class for check in beta_checks])
        self.assertEqual(beta_checks, expected)

    @TestCase.mock_auth(login='test-user')
    def test_component_checks(self):
        request = CreateBetaFromConfig()
        request.MergeFrom(self.full_config)
        with self.mock_request() as ctx:
            self.lineage2.create_beta_from_config(request, ctx)
        with session_scope() as session:
            cid = (
                session.query(model.BetaComponent.id)
                .join(model.yappy__Beta__components)
                .filter(column('yappy__Beta_name') == request.name)
                .scalar()
            )
            component_checks = (
                session.query(model.Check.check_class)
                .filter(model.Check.yappy__BetaComponent_id == cid)
                .all()
            )
        beta_check_prefix = 'beta.'
        expected = sorted([
            check.check_class
            for check in self.full_config.consistency_checks
            if not check.check_class.startswith(beta_check_prefix)
        ])
        component_checks = sorted([check.check_class for check in component_checks])
        self.assertEqual(component_checks, expected)
