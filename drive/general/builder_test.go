package gosql

import (
	"testing"
)

func TestBuilderSelect(t *testing.T) {
	b := NewBuilder(SQLiteDriver)
	inputs := []SelectQuery{
		b.Select("test").Names("id", "key", "value"),
	}
	answers := []string{
		`SELECT "id", "key", "value" FROM "test" WHERE TRUE`,
	}
	for i, input := range inputs {
		if output := input.String(); output != answers[i] {
			t.Fatalf("Expected %q, got %q", answers[i], output)
		}
	}
}
