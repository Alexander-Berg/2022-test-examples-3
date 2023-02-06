import pytest

import filters
from test_utils import create_component


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT), True)
])
def test_only_search_result(component, should_accept):
    assert len(filters.only_search_result([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT), True),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_VIDEO'),
        alignment=filters.Alignments.LEFT
    ), True),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_UNKNOWN'),
        alignment=filters.Alignments.LEFT
    ), False)
])
def test_hide_object_answer_and_geo_wizards(component, should_accept):
    assert len(filters.hide_object_answer_and_geo_wizards([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT), False),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_VIDEO'),
        alignment=filters.Alignments.LEFT
    ), True),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_UNKNOWN'),
        alignment=filters.Alignments.LEFT
    ), False),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_MAPS'),
        alignment=filters.Alignments.LEFT
    ), False),
    (create_component(
        component_type=filters.ComponentTypes.WIZARD,
        wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_KNOWLEDGE_GRAPH'),
        alignment=filters.Alignments.LEFT
    ), False)

])
def test_hide_object_answer_and_geo_wizards_only(component, should_accept):
    assert len(filters.hide_object_answer_and_geo_wizards_only([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                      wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_ADRESA'),
                      alignment=filters.Alignments.LEFT
                      ), True),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                      alignment=filters.Alignments.RIGHT
                      ), False)
])
def test_any_left(component, should_accept):
    assert len(filters.any_left([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), True),
    (create_component(wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_ADRESA')), True),
    (create_component(wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_CALCULATOR')), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                      wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_UNKNOWN'),
                      alignment=filters.Alignments.RIGHT
                      ), True)
])
def test_not_wizard_types_black_listed(component, should_accept):
    assert len(filters.not_wizard_types_black_listed([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('json_component, should_accept', [
    (create_component(), True),
    (create_component(json_slice=['BIATHLON']), False),
    (create_component(json_slice=['NotInSet']), True)
])
def test_not_slices_black_listed(json_component, should_accept):
    assert len(filters.not_slices_black_listed([json_component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('components, len_filtered_components', [
    ([create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_ADRESA'),
                       alignment=filters.Alignments.LEFT),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT,
                       json_slice=['NotInSet']),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT,
                       json_slice=['BIATHLON'])
      ], 3)
])
def test_any_left_black_listed(components, len_filtered_components):
    assert len(filters.any_left_black_listed(components)) == len_filtered_components


@pytest.mark.parametrize('component, should_accept', [
    (create_component(), True),
    (create_component(wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_ADRESA')), False),
    (create_component(wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_KNOWLEDGE_GRAPH')), False),
    (create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                      wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_UNKNOWN'),
                      alignment=filters.Alignments.RIGHT
                      ), True)
])
def test_not_wizard_types_KPI(component, should_accept):
    assert len(filters.not_wizard_types_KPI([component])) == (1 if should_accept else 0)


@pytest.mark.parametrize('json_component, should_accept', [
    (create_component(), True),
    (create_component(json_slice=['ENTITY_SEARCH']), False),
    (create_component(json_slice=['NotInSet']), True)
])
def test_not_slices_KPI(json_component, should_accept):
    assert len(filters.not_slices_KPI([json_component])) == (1 if should_accept else 0)


# 'WIZARD_ADRESA', 'BIATHLON', 'ENTITY_SEARCH' not passed, therefore filter accept 3 of 6 components
@pytest.mark.parametrize('components, len_filtered_components', [
    ([create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_ADRESA'),
                       alignment=filters.Alignments.LEFT),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       wizard_type=filters.WizardTypes.to_metrics_format.get('WIZARD_UNKNOWN'),
                       alignment=filters.Alignments.LEFT),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT,
                       json_slice=['NotInSet']),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT,
                       json_slice=['BIATHLON']),
      create_component(component_type=filters.ComponentTypes.SEARCH_RESULT,
                       alignment=filters.Alignments.LEFT,
                       json_slice=['ENTITY_SEARCH'])
      ], 3)
])
def test_blender_KPI(components, len_filtered_components):
    assert len(filters.blender_KPI(components)) == len_filtered_components
