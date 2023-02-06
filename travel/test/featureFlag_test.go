package test

import (
	"net/http"
	"reflect"
	"sort"
	"testing"

	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/controllers"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/controllers/response"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/indexbuilder"
	"a.yandex-team.ru/travel/avia/feature_flag_api/internal/models"
)

type IndexGetterImpl struct {
	index *indexbuilder.Index
}

func (ig *IndexGetterImpl) GetCurrentIndex() *indexbuilder.Index {
	return ig.index
}

func TestGetByCode_IndexIsNotInitialisedYet(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: nil}
	params := make(map[string][]string)

	status, rawResult := controllers.GetByCode(indexGetter, params)

	if status != http.StatusServiceUnavailable {
		t.Errorf("We expected status: 503, but actual %d", status)
		return
	}

	result, ok := rawResult.(*response.FailFeatureFlag)

	if !ok {
		resultType := "nil"
		if result != nil {
			resultType = reflect.TypeOf(result).String()
		}
		t.Errorf(
			"We expected type controllers.FailResult,"+
				" but actual is %s",
			resultType,
		)
		return
	}

	if result.Message != "Index is not ready" {
		t.Errorf(
			"We expected fail message [Index is not ready], "+
				"but actula fail message is [%s]",
			result.Message,
		)
		return
	}
}

func TestGetByCode_ServiceIsUnknown(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{},
		[]*models.FeatureFlag{},
		[]*models.ServiceFeatureFlagRelation{},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	if status != http.StatusBadRequest {
		t.Errorf("We expected status: %d, but actual %d", http.StatusBadRequest, status)
		return
	}

	result, ok := rawResult.(*response.FailFeatureFlag)

	if !ok {
		resultType := "nil"
		if result != nil {
			resultType = reflect.TypeOf(result).String()
		}
		t.Errorf(
			"We expected type controllers.FailResult,"+
				" but actual is %s",
			resultType,
		)
		return
	}

	if result.Message != "Unknown services" {
		t.Errorf(
			"We expected fail message [Unknown services], "+
				"but actual fail message is [%s]",
			result.Message,
		)
		return
	}
}

func checkResult(t *testing.T, status int, rawResult interface{}, flags, abFlags []string) {
	if status != http.StatusOK {
		t.Errorf("We expected status: %d, but actual %d", http.StatusOK, status)
		return
	}
	result, ok := rawResult.(*response.FeatureFlag)

	if !ok {
		resultType := "nil"
		if result != nil {
			resultType = reflect.TypeOf(result).String()
		}
		t.Errorf(
			"We expected type controllers.Result,"+
				" but actual is %s",
			resultType,
		)
		return
	}
	checkFlags(t, "flags", flags, result.Flags)
	checkFlags(t, "abFlags", abFlags, result.ABFlags)
}

func checkFlags(t *testing.T, flagType string, expectedFlags, flags []string) {
	if len(expectedFlags) != len(flags) {
		t.Errorf(
			"We expected [%v] %s, "+
				"but actula flags are [%v]",
			expectedFlags,
			flagType,
			flags,
		)
		return
	}

	sort.Strings(expectedFlags)
	sort.Strings(flags)

	for i := range expectedFlags {
		if expectedFlags[i] != flags[i] {
			t.Errorf(
				"We expected [%v] %s, "+
					"but actula flags are [%v]",
				expectedFlags,
				flagType,
				flags,
			)
			return
		}
	}
}

func TestGetByCode_ServiceDoesNotHaveSomeFlags(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{{ID: 13, Code: "someServiceCode"}},
		[]*models.FeatureFlag{},
		[]*models.ServiceFeatureFlagRelation{},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	checkResult(t, status, rawResult, []string{}, []string{})
}

func TestGetByCode_ServiceWithDisableFlag(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{{ID: 13, Code: "someServiceCode"}},
		[]*models.FeatureFlag{{ID: 130, Code: "someFeatureCode", State: models.Disable}},
		[]*models.ServiceFeatureFlagRelation{{ServiceID: 13, FeatureFlagID: 130}},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	checkResult(t, status, rawResult, []string{}, []string{})
}

func TestGetByCode_ServiceWithFlags(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{{ID: 13, Code: "someServiceCode"}},
		[]*models.FeatureFlag{{ID: 130, Code: "a", State: models.Enable}, {ID: 1300, Code: "b", State: models.Enable}},
		[]*models.ServiceFeatureFlagRelation{{ServiceID: 13, FeatureFlagID: 130}, {ServiceID: 13, FeatureFlagID: 1300}},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	checkResult(t, status, rawResult, []string{"a", "b"}, []string{})
}

func TestGetByCode_ServiceWithABFlags(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{{ID: 13, Code: "someServiceCode"}},
		[]*models.FeatureFlag{{ID: 130, Code: "a", State: models.UseAB}, {ID: 1300, Code: "b", State: models.UseAB}},
		[]*models.ServiceFeatureFlagRelation{{ServiceID: 13, FeatureFlagID: 130}, {ServiceID: 13, FeatureFlagID: 1300}},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	checkResult(t, status, rawResult, []string{}, []string{"a", "b"})
}

func TestGetByCode_ServiceWithAllFlags(t *testing.T) {
	indexGetter := &IndexGetterImpl{index: (&indexbuilder.Builder{}).Build(
		[]*models.Service{{ID: 13, Code: "someServiceCode"}},
		[]*models.FeatureFlag{
			{ID: 130, Code: "a", State: models.Enable},
			{ID: 1300, Code: "b", State: models.Disable},
			{ID: 13000, Code: "b", State: models.UseAB},
		},
		[]*models.ServiceFeatureFlagRelation{
			{ServiceID: 13, FeatureFlagID: 130},
			{ServiceID: 13, FeatureFlagID: 1300},
			{ServiceID: 13, FeatureFlagID: 13000},
		},
	)}
	params := make(map[string][]string)
	params["service-code"] = []string{"someServiceCode"}

	status, rawResult := controllers.GetByCode(indexGetter, params)

	checkResult(t, status, rawResult, []string{"a"}, []string{"b"})
}
