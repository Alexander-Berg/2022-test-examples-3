package blacklist

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestParse(t *testing.T) {
	assert.ElementsMatch(t,
		[]parsedItem{
			{path: []string{"array"}, value: []int{1, 2, 3}},
			{path: []string{"nested", "operator"}, operator: "$op", value: 1},
			{path: []string{"nested", "value"}, value: "10"},
		},
		parse(map[string]interface{}{
			"array": []int{1, 2, 3},
			"nested": map[string]interface{}{
				"operator": map[string]interface{}{"$op": 1},
				"value":    "10",
				"#comment": "comment",
			},
		}),
	)
}

func TestParseJsonRules(t *testing.T) {
	var (
		rules parsedRules
		err   error
	)

	_, err = parseJSONRules("not a json")
	assert.Error(t, err)

	_, err = parseJSONRules("[1, 2, 3]")
	assert.Error(t, err)

	rules, err = parseJSONRules(`[
		{"bar": 2},
		{"foo": {"$op": 1}}
	]`)
	if assert.NoError(t, err) {
		assert.Equal(t,
			parsedRules{
				[]parsedItem{
					{path: []string{"bar"}, value: float64(2)},
				},
				[]parsedItem{
					{path: []string{"foo"}, operator: "$op", value: float64(1)},
				},
			},
			rules,
		)
	}
}
