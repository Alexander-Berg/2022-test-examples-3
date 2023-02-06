package flags

import (
	"testing"

	"github.com/stretchr/testify/assert"

	flagsPkg "a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/flags"
)

func TestParseFlags_EmptyRawFlags_ReturnsEmpty(t *testing.T) {
	parser := flagsPkg.NewParser()

	flags, err := parser.Parse("")

	assert.NoError(t, err)
	assert.Equal(t, flags, flagsPkg.Flags{})
}

func TestParseFlags_BadFormat_ReturnsFlagParsingError(t *testing.T) {
	parser := flagsPkg.NewParser()

	test := func(rawFlags string) func(*testing.T) {
		return func(t *testing.T) {
			_, err := parser.Parse(rawFlags)

			assert.Error(t, err)
			assert.IsType(t, err, &flagsPkg.FlagParsingError{})
		}
	}

	t.Run("flag==", test("flag=="))
	t.Run("flag=1,,", test("flag=1,,"))
	t.Run("=flag", test("=flag"))
}

func TestParseFlags_GoodFormat_ReturnsFlags(t *testing.T) {
	parser := flagsPkg.NewParser()

	test := func(rawFlags string, expectedFlags flagsPkg.Flags) func(*testing.T) {
		return func(t *testing.T) {
			actualFlags, err := parser.Parse(rawFlags)

			assert.NoError(t, err)
			assert.Equal(t, expectedFlags, actualFlags)
		}
	}

	t.Run("flag=", test("flag=", map[string]string{"flag": ""}))
	t.Run("flag=val1", test("flag=val1", map[string]string{"flag": "val1"}))
	t.Run(
		"flag1=val1,flag2,flag3=val3",
		test("flag1=val1,flag2,flag3=val3", map[string]string{"flag1": "val1", "flag2": "", "flag3": "val3"}),
	)
	t.Run(
		"flag1=val1,flag1=val2",
		test("flag1=val1,flag1=val2", map[string]string{"flag1": "val2"}),
	)
}
