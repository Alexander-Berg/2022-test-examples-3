package templater

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

type Data struct {
	FromStationsTitles []string
}

func TestJoinLastFilter(t *testing.T) {
	baseTemplate, err := InitTemplate("{{FromStationsTitles|join_last:\" и \"|join:\", \"}}")
	assert.NoError(t, err)
	tests := []struct {
		stTitles []string
		expected string
	}{
		{
			[]string{
				"Ленинградский вокзал",
				"Киевский вокзал",
				"Восточный вокзал",
				"Курский вокзал",
			},
			"Ленинградский вокзал, Киевский вокзал, Восточный вокзал и Курский вокзал",
		},
		{
			[]string{
				"Восточный вокзал",
				"Курский вокзал",
			},
			"Восточный вокзал и Курский вокзал",
		},
		{
			[]string{
				"Восточный вокзал",
			},
			"Восточный вокзал",
		},
		{
			[]string{},
			"",
		},
	}
	for _, tt := range tests {
		testname := fmt.Sprintf("baseTemplateListOf%d", len(tt.stTitles))
		t.Run(testname, func(t *testing.T) {
			data := Data{FromStationsTitles: tt.stTitles}
			result, err := ExecuteTemplate(baseTemplate, data)
			assert.NoError(t, err)
			assert.Equal(t, tt.expected, result)
		})
	}

	t.Run("noArgs", func(t *testing.T) {
		data1 := Data{
			FromStationsTitles: []string{
				"Восточный вокзал",
				"Курский вокзал",
				". The End.",
			},
		}
		tmplt1, err := InitTemplate("{{FromStationsTitles|join_last|join:\", \"}}")
		assert.NoError(t, err)
		result, err := ExecuteTemplate(tmplt1, data1)
		assert.NoError(t, err)
		assert.Equal(t, "Восточный вокзал, Курский вокзал. The End.", result)
	})
	t.Run("joinLast2", func(t *testing.T) {
		data1 := Data{
			FromStationsTitles: []string{
				"Киевский вокзал",
				"Восточный вокзал",
				"Курский вокзал",
			},
		}
		tmplt1, err := InitTemplate("{{FromStationsTitles|join_last:\" и еще \"|join_last:\" и \"|join:\", \"}}")
		assert.NoError(t, err)
		result, err := ExecuteTemplate(tmplt1, data1)
		assert.NoError(t, err)
		assert.Equal(t, "Киевский вокзал и Восточный вокзал и еще Курский вокзал", result)
	})
}
