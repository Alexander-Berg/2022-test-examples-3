package main

import (
	"net"
	"reflect"
	"testing"
)

func TestHostsEntryFromLine(t *testing.T) {
	type args struct {
		line string
	}
	tests := []struct {
		name    string
		args    args
		want    *HostsEntry
		wantErr bool
	}{
		{
			name: "empty",
			args: args{
				line: "",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "wrong ip",
			args: args{
				line: "123 some.name",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "with one name",
			args: args{
				line: "192.168.0.1 some.name",
			},
			want: &HostsEntry{
				ip:    net.ParseIP("192.168.0.1"),
				names: []string{"some.name"},
			},
			wantErr: false,
		},
		{
			name: "with multiple names",
			args: args{
				line: "192.168.0.1 some.name some some.name.gtld",
			},
			want: &HostsEntry{
				ip:    net.ParseIP("192.168.0.1"),
				names: []string{"some.name", "some", "some.name.gtld"},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := HostsEntryFromLine(tt.args.line)
			if (err != nil) != tt.wantErr {
				t.Errorf("HostsEntryFromLine() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("HostsEntryFromLine() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetStaticEntries(t *testing.T) {
	type args struct {
		lines    []byte
		hostname string
	}
	tests := []struct {
		name    string
		args    args
		want    []*HostsEntry
		wantErr bool
	}{
		{
			name: "empty",
			args: args{
				lines: []byte(""),
			},
			want:    make([]*HostsEntry, 0),
			wantErr: false,
		},
		{
			name: "without static",
			args: args{
				lines: []byte(`# static entries

127.0.0.1 localhost localhost.localdomain localhost4 localhost4.localdomain4
::1 localhost localhost.localdomain localhost6 localhost6.localdomain6

# generated entries

2a02:6b8:b010:5026:225:90ff:fe9a:8adc pepelac01e.market.yandex.net pepelac01e
93.158.141.30 pepelac01e.market.yandex.net pepelac01e`),
				hostname: "pepelac01e.market.yandex.net",
			},
			want:    make([]*HostsEntry, 0),
			wantErr: false,
		},
		{
			name: "with static",
			args: args{
				lines: []byte(`# static entries

2a02:6b8:b010:5026:225:90ff:fe9a:8adc another.market.yandex.net another
127.0.0.1 localhost localhost.localdomain localhost4 localhost4.localdomain4
::1 localhost localhost.localdomain localhost6 localhost6.localdomain6

# generated entries

2a02:6b8:b010:5026:225:90ff:fe9a:8adc pepelac01e.market.yandex.net pepelac01e
93.158.141.30 pepelac01e.market.yandex.net pepelac01e`),
				hostname: "pepelac01e.market.yandex.net",
			},
			want: []*HostsEntry{{
				ip:    net.ParseIP("2a02:6b8:b010:5026:225:90ff:fe9a:8adc"),
				names: []string{"another.market.yandex.net", "another"},
			}},
			wantErr: false,
		},
		{
			name: "with static broken line",
			args: args{
				lines: []byte(`# static entries

2a02:6b8:b010:5026:225:90ff:fe9a:8adr another.market.yandex.net another
127.0.0.1 localhost localhost.localdomain localhost4 localhost4.localdomain4
::1 localhost localhost.localdomain localhost6 localhost6.localdomain6

# generated entries

2a02:6b8:b010:5026:225:90ff:fe9a:8adc pepelac01e.market.yandex.net pepelac01e
93.158.141.30 pepelac01e.market.yandex.net pepelac01e`),
				hostname: "pepelac01e.market.yandex.net",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "with local",
			args: args{
				lines: []byte(`::1 localhost localhost.localdomain

127.0.0.1 localhost localhost.localdomain
93.158.141.30 pepelac01e.market.yandex.net pepelac01e
2a02:6b8:b010:5026:225:90ff:fe9a:8add pepelac01e.market.yandex.net pepelac01e
fe80:6b8:b010:5026:225:90ff:fe9a:8adc some.name
`),
				hostname: "pepelac01e.market.yandex.net",
			},
			want:    make([]*HostsEntry, 0),
			wantErr: false,
		},
		{
			name: "with gray",
			args: args{
				lines: []byte(`::1 localhost localhost.localdomain

127.0.0.1 localhost localhost.localdomain
93.158.141.30 pepelac01e.market.yandex.net pepelac01e
2a02:6b8:b010:5026:225:90ff:fe9a:8add pepelac01e.market.yandex.net pepelac01e
10.0.1.1 some.name
`),
				hostname: "pepelac01e.market.yandex.net",
			},
			want: []*HostsEntry{{
				ip:    net.ParseIP("10.0.1.1"),
				names: []string{"some.name"},
			}},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getStaticEntries(tt.args.lines, tt.args.hostname)
			if (err != nil) != tt.wantErr {
				t.Errorf("getStaticEntries() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getStaticEntries() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGenerateHostsContent(t *testing.T) {
	type args struct {
		entries []*HostsEntry
		tag     string
	}
	tests := []struct {
		name string
		args args
		want []byte
	}{
		{
			name: "empty",
			args: args{
				entries: nil,
				tag:     localEntriesTag,
			},
			want: make([]byte, 0),
		},
		{
			name: "local",
			args: args{
				entries: localEntries,
				tag:     localEntriesTag,
			},
			want: []byte(`# local entries
127.0.0.1 localhost localhost.localdomain localhost4 localhost4.localdomain4
::1 localhost localhost.localdomain localhost6 localhost4.localdomain6
# local entries
`),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := entriesHostsContent(tt.args.entries, tt.args.tag); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("entriesHostsContent() = %v, want %v", got, tt.want)
			}
		})
	}
}
