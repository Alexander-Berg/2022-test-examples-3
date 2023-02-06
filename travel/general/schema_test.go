package bitrix

import (
	"a.yandex-team.ru/travel/budapest/bitrix_sync/pkg/bitrix/client"
	"reflect"
	"testing"
)

func TestCorrectWellKnownFields(t *testing.T) {
	type wellKnownFields struct {
		Foo client.DealField
		Bar client.DealField
	}

	basicConfig := Config{
		DealFields: map[string]string{
			"Foo": "FOO_FIELD",
			"Bar": "BAR_FIELD",
		},
	}

	var wkf wellKnownFields
	assertNil(t, LoadWellKnownFields(basicConfig, &wkf), "")
	assertEqual(t, wkf.Foo, client.DealField("FOO_FIELD"), "")
	assertEqual(t, wkf.Bar, client.DealField("BAR_FIELD"), "")
}

func TestNonConfiguredWellKnownField(t *testing.T) {
	type wellKnownFields struct {
		Foo client.DealField
		Bar client.DealField
	}

	basicConfig := Config{
		DealFields: map[string]string{
			"Foo": "FOO_FIELD",
		},
	}

	var wkf wellKnownFields
	expectedError := NonConfiguredWellKnownField{FieldName: "Bar"}
	assertEqual(t, LoadWellKnownFields(basicConfig, &wkf), expectedError, "non-configurable wkf mismatch")
}

func TestInvalidWellKnownField(t *testing.T) {
	type wellKnownFields struct {
		Foo []string
		Bar client.DealField
	}

	basicConfig := Config{
		DealFields: map[string]string{
			"Foo": "FOO_FIELD",
			"Bar": "BAR_FIELD",
		},
	}
	var wkf wellKnownFields
	expectedError := InvalidWellKnownField{FieldName: "Foo", FieldType: reflect.TypeOf([]string{})}
	assertEqual(t, LoadWellKnownFields(basicConfig, &wkf), expectedError, "invalid wkf mismatch")
}

func TestCorrectWellKnownStages(t *testing.T) {
	type wellKnownStages struct {
		Qux client.DealStage
		Baz client.DealStage
	}

	basicConfig := Config{
		DealStages: map[string]string{
			"Qux": "QUX_STAGE",
			"Baz": "BAZ_STAGE",
		},
	}

	var wks wellKnownStages
	assertNil(t, LoadWellKnownStages(basicConfig, &wks), "")
	assertEqual(t, wks.Qux, client.DealStage("QUX_STAGE"), "")
	assertEqual(t, wks.Baz, client.DealStage("BAZ_STAGE"), "")
}

func TestNonConfiguredWellKnownStages(t *testing.T) {
	type wellKnownStages struct {
		Qux client.DealStage
		Baz client.DealStage
	}

	basicConfig := Config{
		DealStages: map[string]string{
			"Qux": "QUX_STAGE",
		},
	}

	var wks wellKnownStages
	expectedError := NonConfiguredWellKnownStage{StageName: "Baz"}
	assertEqual(t, LoadWellKnownStages(basicConfig, &wks), expectedError, "non-configurable wks mismatch")
}

func TestInvalidWellKnownStages(t *testing.T) {
	type wellKnownStages struct {
		Qux string
		Baz client.DealStage
	}

	basicConfig := Config{
		DealStages: map[string]string{
			"Qux": "QUX_STAGE",
			"Baz": "BAZ_STAGE",
		},
	}

	var wks wellKnownStages
	expectedError := InvalidWellKnownStage{FieldName: "Qux", FieldType: reflect.TypeOf("")}
	assertEqual(t, LoadWellKnownStages(basicConfig, &wks), expectedError, "non-configurable wks mismatch")
}

func assertEqual(t *testing.T, actual interface{}, expected interface{}, message string) {
	if message == "" {
		message = "assertion failed"
	}
	if reflect.DeepEqual(actual, expected) {
		return
	}
	t.Errorf("%s: received '%v' (type '%v'), expected '%v' (type '%v')", message, actual, reflect.TypeOf(actual), expected, reflect.TypeOf(expected))
}

func assertNil(t *testing.T, actual interface{}, message string) {
	if message == "" {
		message = "assertion failed"
	}
	if actual == nil {
		return
	}
	t.Errorf("%s expected nil value, got '%v' instead", message, actual)
}

func TestStandardSchemaDeserialization(t *testing.T) {
	type deal struct {
		ID    int    `bitrix:"ID"`
		Title string `bitrix:"TITLE_FIELD"`
	}
	s, err := LoadMappingSchema(Config{}, reflect.TypeOf(deal{}))
	assertNil(t, err, "")
	assertEqual(t, s.AllFields, []client.DealField{"ID", "TITLE_FIELD"}, "")
	resp := map[string]interface{}{
		"ID":          42,
		"TITLE_FIELD": "Some Name",
	}
	res, err := s.MapToDeal(resp, nil)
	assertNil(t, err, "")
	result := res.(deal)
	assertEqual(t, result.ID, 42, "")
	assertEqual(t, result.Title, "Some Name", "")
}

func TestConfigurableSchemaDeserialization(t *testing.T) {
	type deal struct {
		ID    int    `bitrix:"ID"`
		Title string `bitrix:"!"`
	}
	s, err := LoadMappingSchema(Config{
		DealFields: map[string]string{
			"Title": "USER_FIELD_450",
		},
	}, reflect.TypeOf(deal{}))
	assertNil(t, err, "")
	assertEqual(t, s.AllFields, []client.DealField{"ID", "USER_FIELD_450"}, "")
	resp := map[string]interface{}{
		"ID":             42,
		"USER_FIELD_450": "Some Name",
	}
	res, err := s.MapToDeal(resp, nil)
	assertNil(t, err, "")
	result := res.(deal)
	assertEqual(t, result.ID, 42, "")
	assertEqual(t, result.Title, "Some Name", "")
}
