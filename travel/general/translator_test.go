package tanker

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTranslator_TemplateToString(t *testing.T) {
	t.Run(
		"simple substitution", func(t *testing.T) {
			params := map[string]interface{}{
				"city": "Moscow",
			}
			result, err := TemplateToString("test", "This is {{.city}}", params)
			assert.NoError(t, err)
			assert.Equal(t, "This is Moscow", result)
		},
	)

	t.Run(
		"conditional substitutions", func(t *testing.T) {
			template := "Hi{{if .name}}, {{.name}}{{else}} there{{end}}!"

			paramsWithName := map[string]interface{}{
				"name": "Peter",
			}
			result, err := TemplateToString("test", template, paramsWithName)
			assert.NoError(t, err)
			assert.Equal(t, "Hi, Peter!", result)

			paramsWithNoName := map[string]interface{}{
				"name": "",
			}
			result, err = TemplateToString("test", template, paramsWithNoName)
			assert.NoError(t, err)
			assert.Equal(t, "Hi there!", result)
		},
	)

	t.Run(
		"incorrect template", func(t *testing.T) {
			params := map[string]interface{}{
				"city": "Moscow",
			}
			result, err := TemplateToString("test", "this is {{.city}", params)
			assert.Error(t, err)
			assert.Equal(t, "", result)
		},
	)
}
