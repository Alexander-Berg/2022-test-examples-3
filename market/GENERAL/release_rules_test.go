package yptool

import (
	"a.yandex-team.ru/market/sre/library/golang/deplate/values"
	"a.yandex-team.ru/yp/go/yson/ypapi"
	"github.com/stretchr/testify/assert"
	"testing"
)

func stringPointer(s string) *string {
	return &s
}

func TestGetReleaseEnvironmentMap(t *testing.T) {
	envStage := GetReleaseEnvironmentMap()
	for _, env := range values.AllEnvironments() {
		if _, ok := envStage[env]; !ok {
			t.Errorf("%s environment missing in map", env)
		}
	}
	for _, env := range values.AllSandboxEnvironments() {
		if _, ok := envStage[env]; !ok {
			t.Errorf("%s environment missing in map", env)
		}
	}
}

func TestFilterReleaseRules(t *testing.T) {
	oldRules := map[string]*ypapi.TReleaseRule{
		"toUpdate": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_update"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType1"),
					ResourceTypes: []string{"resourceType1"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["production"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType1"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit1"),
								LayerRef:     stringPointer("layer1"),
							},
						},
					},
				},
			},
		},
		"toKeep": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_keep"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType2"),
					ResourceTypes: []string{"resourceType2"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["production"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType2"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit2"),
								LayerRef:     stringPointer("layer2"),
							},
						},
					},
				},
			},
		},
		"toDelete": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_delete"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType3"),
					ResourceTypes: []string{"resourceType3"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["production"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType3"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit3"),
								LayerRef:     stringPointer("layer3"),
							},
						},
					},
				},
			},
		},
	}

	newRules := map[string]*ypapi.TReleaseRule{
		"toUpdate": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_update"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType1"),
					ResourceTypes: []string{"resourceType1"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["testing"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType1"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit1"),
								LayerRef:     stringPointer("layer1"),
							},
						},
					},
				},
			},
		},
		"toKeep": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_keep2"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType2"),
					ResourceTypes: []string{"resourceType2"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["production"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType2"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit2"),
								LayerRef:     stringPointer("layer2"),
							},
						},
					},
				},
			},
		},
		"toCreate": {
			Meta: &ypapi.TReleaseRuleMeta{
				Id: stringPointer("meta_to_create"),
			},
			Spec: &ypapi.TReleaseRuleSpec{
				Sandbox: &ypapi.TSandboxSelector{
					TaskType:      stringPointer("taskType4"),
					ResourceTypes: []string{"resourceType4"},
					ReleaseTypes:  []string{GetReleaseEnvironmentMap()["production"]},
				},
				Patches: map[string]*ypapi.TDeployPatchSpec{
					"patch": {
						Sandbox: &ypapi.TSandboxResourceDeployPatch{
							SandboxResourceType: stringPointer("resourceType4"),
							Static: &ypapi.TStaticResourceRef{
								DeployUnitId: stringPointer("unit4"),
								LayerRef:     stringPointer("layer4"),
							},
						},
					},
				},
			},
		},
	}

	expectedDiff := &ReleaseRulesDiff{
		Update: map[string]*ypapi.TReleaseRule{"toUpdate": newRules["toUpdate"]},
		Create: map[string]*ypapi.TReleaseRule{"toCreate": newRules["toCreate"]},
		Delete: map[string]*ypapi.TReleaseRule{"toDelete": oldRules["toDelete"]},
	}

	diff := FilterReleaseRules(oldRules, newRules)

	assert.Equal(t, expectedDiff, diff)
}

func TestGenerateReleaseRuleName(t *testing.T) {
	ruleName, err := GenerateReleaseRuleName("testing_market_test-stage-for-tests", "deploy-unit", "some-resource1234")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "testing_market_test-stage-for-tests_deploy-unit_some-resource1234_rule", ruleName)

	ruleName, err = GenerateReleaseRuleName("testing_market_test-stage-for-tests", "deploy-unit", "some-resource12345")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "testing_market_test-stage-for-tests_d-u_some-resource12345_rule", ruleName)

	ruleName, err = GenerateReleaseRuleName("testing_market_test-stage-for-tests", "deploy-unit", "some-resource-abcdef-12345")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "testing_market_test-stage-for-tests_d-u_some-resource-abcdef-12345", ruleName)

	ruleName, err = GenerateReleaseRuleName("testing_market_test-stage-for-tests", "deploy-unit", "some-resource-zyxw-abcdef-12345")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "testing_market_test-stage-for-tests_d-u_s-r-z-a-1", ruleName)

	ruleName, err = GenerateReleaseRuleName("testing_market_test-stage-for-tests-zyxwuts-abcdefg-12345", "deploy-unit", "some-resource-zyxw-abcdef-12345")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "t_market_test-stage-for-tests-zyxwuts-abcdefg-12345_d-u_s-r-z-a-1", ruleName)

	ruleName, err = GenerateReleaseRuleName("_market_test-stage-for-tests-zyxwutsrqp-abcdefghij-123456", "deploy-unit", "some-resource-zyxw-abcdef-12345")
	assert.NoErrorf(t, err, "Unexpected error")
	assert.Equal(t, "fb6c5443ee0546444eda5d6b1e23f252ebfc5a781371aff9d3e0ed90195816c4", ruleName)
	t.Log(ruleName)
}
