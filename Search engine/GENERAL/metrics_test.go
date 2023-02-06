package model

import "testing"

var NormalizeNamePartTests = []struct {
	In  string
	Out string
}{
	{
		"f935ae95-e1e9-4e76-b23b-7963a6fb539a",
		"f935ae95_e1e9_4e76_b23b_7963a6fb539a",
	},
	{
		"rex.host",
		"rex_host",
	},
	{
		"127.0.0.1",
		"127_0_0_1",
	},
}

func TestNormalizeNamePart(t *testing.T) {
	for _, tt := range NormalizeNamePartTests {
		if s := normalizeNamePart(tt.In); s != tt.Out {
			t.Errorf("want %q, got %q", tt.Out, s)
		}
	}
}
