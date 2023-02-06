package internal

import (
	"reflect"
	"testing"
)

func TestFindHostInCluster(t *testing.T) {
	testCluster := Cluster{"testCluster", []string{"host1", "host2"}}

	if !testCluster.hasTargetHost("host1") {
		t.Error("expected cluster has host1, but actually it hasn't")
	}

	if !testCluster.hasTargetHost("host2") {
		t.Error("expected cluster has host2, but actually it hasn't")
	}
}

func TestShouldNotFoundHostInCluster(t *testing.T) {
	testCluster := Cluster{"testCluster", []string{"host1", "host2"}}

	if testCluster.hasTargetHost("host3") {
		t.Error("cluster shouldn't has host3, but actually it has")
	}
}

func TestHostsExceptTargetShouldReturnOtherHosts(t *testing.T) {
	testCluster := Cluster{"testCluster", []string{"host1", "host2"}}

	if !reflect.DeepEqual([]string{"host2"}, testCluster.hostsExceptTarget("host1")) {
		t.Error("hostsExceptTarget should return host2, but actually it hasn't")
	}
}

func TestHostsExceptTargetFailedIfReturnSameHosts(t *testing.T) {
	testCluster := Cluster{"testCluster", []string{"host1", "host2"}}

	if reflect.DeepEqual([]string{"host1"}, testCluster.hostsExceptTarget("host1")) {
		t.Error("hostsExceptTarget should return host2, but actually it hasn't")
	}
}

func TestNeighborHosts() {

}
