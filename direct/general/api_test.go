package cloudapi

import (
	"encoding/json"
	"io/ioutil"
	"path/filepath"
	"testing"
)

var (
	goodhost = "vla-hoclw1l184ut1uet.db.yandex.net"
	badhost  = "vla1-hoclw1l184ut1uet.db.yandex.net"
)

func TestClusterHosts(t *testing.T) {
	testFile, _ := filepath.Abs("./cloud_shards_testing.json")
	data, err := ioutil.ReadFile(testFile)
	if err != nil {
		t.Fatal("error read file")
	}

	var cluster ClusterHosts //список сылок на cluster хосты
	if err := json.Unmarshal(data, &cluster); err != nil {
		t.Fatalf("data %s: error %s", data, err)
	}
	hostnames := cluster.Hostnames()

	if ok := hostnames.HasHost(goodhost); !ok {
		t.Fatalf("good host %s not found in %s", goodhost, hostnames)
	}

	if ok := hostnames.HasHost(badhost); ok {
		t.Fatalf("bad host %s found in %s", badhost, hostnames)
	}
}
