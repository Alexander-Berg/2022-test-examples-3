package dynamic

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters/dynamic"
)

var (
	aviaDynamicParser = dynamic.AviaDynamicParser{}
)

func checkAviaDynamicValue(aviaDynamic *dynamic.AviaDynamic, expectedContext dynamic.Context, t *testing.T) {
	if aviaDynamic.Context == nil {
		t.Error("Context is nil")
		return
	}

	if len(aviaDynamic.Context) != len(expectedContext) {
		t.Errorf("Expected context size: %v. Found context size: %v", len(aviaDynamic.Context), len(expectedContext))
		return
	}

	eq := reflect.DeepEqual(aviaDynamic.Context, expectedContext)
	if !eq {
		t.Error("Actual context is not equal to expected.")
	}
}

func TestParserShouldReturnNilIfInputIsEmpty(t *testing.T) {
	rawAviaDynamic := ""

	aviaDynamic, err := aviaDynamicParser.Parse(rawAviaDynamic)
	assert.Nil(t, aviaDynamic)
	assert.NoError(t, err)
}

func TestParserShouldBeAbleToParseCorrectNonEmptyAviaDynamic(t *testing.T) {
	rawAviaDynamic := "{\"Context\":{\"abc\":\"def\", \"123\":\"456\"}}"
	expectedContext := dynamic.Context{"abc": "def", "123": "456"}

	aviaDynamic, err := aviaDynamicParser.Parse(rawAviaDynamic)
	checkAviaDynamicValue(aviaDynamic, expectedContext, t)
	assert.NoError(t, err)
}

func TestParserShouldReturnErrorInCaseOfWrongAviaDynamic(t *testing.T) {
	rawAviaDynamic := "ABC 123"

	aviaDynamic, err := aviaDynamicParser.Parse(rawAviaDynamic)
	assert.Nil(t, aviaDynamic)
	assert.Error(t, err)
}
