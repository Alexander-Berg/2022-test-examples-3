package tests

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/funcnames"
	"a.yandex-team.ru/travel/library/go/funcnames/tests/nested"
)

type fnNames struct {
	expect string
	actual funcnames.Caller
}

func TestGetFnName(t *testing.T) {
	for _, names := range []fnNames{
		{"nested.GetPublicFnName", nested.PublicFnName},
		{"nested.getPrivateFnName", nested.PrivateFnName},
		{"nested.TestedStruct.GetPublicFnName", nested.PublicMethodName},
		{"nested.TestedStruct.getPrivateFnName", nested.PrivateMethodName},
		{"nested.TestedStruct.GetPtrMethodName", nested.PtrMethodName},
	} {
		assert.Equal(t, names.expect, names.actual.String())
	}
}

func TestGetFullFnName(t *testing.T) {
	const pkgPath = "library.go.funcnames.tests"

	for _, names := range []fnNames{
		{"nested.GetPublicFnName", nested.FullPublicFnName},
		{"nested.getPrivateFnName", nested.FullPrivateFnName},
		{"nested.TestedStruct.GetPublicFnName", nested.FullPublicMethodName},
		{"nested.TestedStruct.getPrivateFnName", nested.FullPrivateMethodName},
		{"nested.TestedStruct.GetPtrMethodName", nested.FullPtrMethodName},
	} {
		assert.Equal(t, fmt.Sprintf("%s.%s", pkgPath, names.expect), names.actual.String())
	}
}
