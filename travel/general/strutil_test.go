package strutil

import "testing"

func TestCoalesce(t *testing.T) {
	type args struct {
		strings []string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			"empty input",
			args{},
			"",
		},
		{
			"single empty value",
			args{[]string{""}},
			"",
		},
		{
			"single non-empty value",
			args{[]string{"abc"}},
			"abc",
		},
		{
			"multiple values, first is empty",
			args{[]string{"", "abc"}},
			"abc",
		},
		{
			"multiple empty values",
			args{[]string{"", "", "", ""}},
			"",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := Coalesce(tt.args.strings...); got != tt.want {
				t.Errorf("Coalesce() = %v, want %v", got, tt.want)
			}
		})
	}
}
