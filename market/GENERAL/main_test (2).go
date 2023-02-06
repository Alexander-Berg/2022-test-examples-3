package main

import (
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
	"time"
)

func prepare() {
	_ = os.MkdirAll("data/cores", 0777)
	_ = os.MkdirAll("data/empty_cores", 0777)
	file, _ := os.Create("data/cores/core1")
	_ = file.Close()
	file, _ = os.Create("data/cores/core2")
	_ = file.Close()
	file, _ = os.Create("data/cores/core3")
	_ = file.Close()

	now := time.Now()
	_ = os.Chtimes("data/cores/core3", now.AddDate(0, 0, -2), now.AddDate(0, 0, -2))
}

func shutdown() {
	_ = os.RemoveAll("data")
}

func Test_isCoresDirExist(t *testing.T) {
	prepare()
	defer shutdown()
	type args struct {
		path string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "unexist dir",
			args: args{
				path: "some_unexist",
			},
			want: false,
		},
		{
			name: "empty dir",
			args: args{
				path: "data/cores",
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := isCoresDirExist(tt.args.path); got != tt.want {
				t.Errorf("isCoresDirExist() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_findCores(t *testing.T) {
	prepare()
	defer shutdown()
	type args struct {
		path string
	}
	tests := []struct {
		name         string
		args         args
		wantOldLen   int
		wantFreshLen int
	}{
		{
			name: "empty",
			args: args{
				path: "data/empty_cores",
			},
			wantOldLen:   0,
			wantFreshLen: 0,
		},
		{
			name: "good",
			args: args{
				path: "data/cores",
			},
			wantOldLen:   1,
			wantFreshLen: 2,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotOld, gotFresh := findCores(tt.args.path)
			assert.Len(t, gotOld, tt.wantOldLen)
			assert.Len(t, gotFresh, tt.wantFreshLen)
		})
	}
}
