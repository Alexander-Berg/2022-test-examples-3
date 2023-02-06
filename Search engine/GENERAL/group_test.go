package smtp

import (
	"sort"
	"strings"
	"testing"

	"rex/config"
)

var ComputeKeyTests = []struct {
	To []string
	S  string
}{
	{nil, ""},
	{[]string{"a"}, "a"},
	{[]string{"b", "a", "c"}, "abc"},
}

func TestComputeKey(t *testing.T) {
	for _, tt := range ComputeKeyTests {
		if s := computeKey(tt.To); s != tt.S {
			t.Errorf("%s: want %q, got %q", tt.To, tt.S, s)
		}
	}
}

type email struct {
	to   []string
	body string
}

func (e *email) To() []config.Secret {
	var v []config.Secret
	for _, s := range e.to {
		t, _ := config.Email(s)
		v = append(v, t)
	}
	return v
}
func (e *email) Body() string { return e.body }

var GroupsTests = []struct {
	In  []email
	Out []email
}{
	{
		[]email{
			{[]string{"a"}, "b"},
			{[]string{"b"}, "c"},
			{[]string{"a"}, "a"},
			{[]string{"b"}, "d"},
		},
		[]email{
			{[]string{"a"}, "a\n\nb"},
			{[]string{"b"}, "c\n\nd"},
		},
	},
}

func TestGroups_returnValidEmailOnReset(t *testing.T) {
	for _, tt := range GroupsTests {
		g := newGroups()
		for _, e := range tt.In {
			g.Group(&e)
		}
		out := g.Reset()
		sort.Slice(out, func(i, j int) bool {
			to1 := config.Unsafe(out[i].To())
			to2 := config.Unsafe(out[j].To())
			sort.Strings(to1)
			sort.Strings(to2)

			a := strings.Join(to1, "")
			b := strings.Join(to2, "")
			return strings.Compare(a, b) < 0
		})

		if len(out) != len(tt.Out) {
			t.Fatalf("want %d, got %d", len(tt.Out), len(out))
		}

		for i, v := range tt.Out {
			t1 := strings.Join(config.Unsafe(v.To()), " ")
			t2 := strings.Join(config.Unsafe(out[i].To()), " ")
			b1, b2 := v.Body(), out[i].Body()

			if t1 != t2 {
				t.Errorf("to: want %q, got %q", t1, t2)
			}
			if b1 != b2 {
				t.Errorf("body: want %q, got %q", b1, b2)
			}
		}
	}
}
