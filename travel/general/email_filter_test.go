package rollout

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestEmailRulesFilter(t *testing.T) {
	newRule := func(expr string) EmailRule {
		rule, err := NewEmailRule(expr)
		if err != nil {
			panic(err)
		}
		return *rule
	}

	t.Run(
		"Allows/Both banned and allowed should be filtered out", func(t *testing.T) {
			rules := EmailRules{
				Allowed: []EmailRule{newRule("^test@com$")},
				Banned:  []EmailRule{newRule("^test@com$")},
			}
			filter := NewEmailFilter(rules)

			require.False(t, filter.Allows("test@com"))
		},
	)

	t.Run(
		"Allows/Always allowed rules have bigger priority than banned", func(t *testing.T) {
			rules := EmailRules{
				AlwaysAllowed: []EmailRule{newRule("^test@com$")},
				Banned:        []EmailRule{newRule("^test@com$")},
			}
			filter := NewEmailFilter(rules)

			require.True(t, filter.Allows("test@com"))

		},
	)

	t.Run(
		"Allows/allowed should not be filtered out", func(t *testing.T) {
			rules := EmailRules{
				Allowed: []EmailRule{newRule("^test@com$")},
			}
			filter := NewEmailFilter(rules)

			require.True(t, filter.Allows("test@com"))
		},
	)

	t.Run(
		"Allows/allowed by domain should not be filtered out", func(t *testing.T) {
			rules := EmailRules{
				Allowed: []EmailRule{newRule(`.+@domain\.com$`)},
			}
			filter := NewEmailFilter(rules)

			require.True(t, filter.Allows("test@domain.com"))
		},
	)

	t.Run(
		"Allows/email is neither in AllowedAlways nor in Allowed should be filtered", func(t *testing.T) {
			rules := EmailRules{
				AlwaysAllowed: []EmailRule{newRule(`another@domain.com`)},
				Allowed:       []EmailRule{newRule(`test@doma1n.com`)},
			}
			filter := NewEmailFilter(rules)

			require.False(t, filter.Allows("test@domain.com"))
		},
	)
}
