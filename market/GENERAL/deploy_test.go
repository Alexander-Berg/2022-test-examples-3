package loader

import (
	"a.yandex-team.ru/library/go/yandex/deploy/podagent"
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

var testData1 = `
{
  "node_meta": {
    "cluster": "sas.yp.yandex.net",
    "dc": "sas",
    "fqdn": "sas1-1111.search.yandex.net"
  },
  "metadata": {
    "annotations": {},
    "pod_id": "xxxxxxxxxx",
    "labels": {
      "market_env": "testing",
      "market_sox": %s,
      "test_label": "1",
      "test_label_2": 1,
      "test_label_3": true,
      "deploy": {
        "stage_id": "testing_market_project",
        "project_id": "market-sre",
        "deploy_unit_id": "sas"
      },
      "deploy_engine": "MCRSC"
    }
  },
  "ip6_address_allocations": [
    {
      "address": "::0",
      "persistent_fqdn": "xxxxxxxxxx.sas.yp-c.yandex.net",
      "transient_fqdn": "sas1-1111-1.xxxxxxxxxx.sas.yp-c.yandex.net",
      "virtual_services": [],
      "vlan_id": "backbone",
      "labels": {}
    }
  ],
  "box_resource_requirements": {
    "box": {
      "cpu": {
        "cpu_limit_millicores": 0,
        "cpu_guarantee_millicores": 0
      },
      "memory": {
        "memory_guarantee_bytes": 0,
        "memory_limit_bytes": 0
      }
    },
    "logbroker_tools_box": {
      "cpu": {
        "cpu_limit_millicores": 400,
        "cpu_guarantee_millicores": 400
      },
      "memory": {
        "memory_guarantee_bytes": 536870912,
        "memory_limit_bytes": 536870912
      }
    }
  },
  "resource_requirements": {
    "cpu": {
      "cpu_limit_millicores": 2222,
      "cpu_guarantee_millicores": 555
    },
    "memory": {
      "memory_guarantee_bytes": 3333,
      "memory_limit_bytes": 4444
    }
  },
  "disk_volume_allocations": []
}
`

func TestIsDeploy(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(podagent.EnvPodIDKey)
	}
	clearEnv()
	defer clearEnv()

	isDeploy := IsDeploy()
	assert.False(t, isDeploy)

	err := os.Setenv(podagent.EnvPodIDKey, "1")
	require.NoError(t, err)

	isDeploy = IsDeploy()
	assert.True(t, isDeploy)
}

func TestNewDeployLoader(t *testing.T) {
	ctx := context.Background()

	appName := "test_app"
	d := NewDeployLoader(ctx, appName).(*DeployLoader)

	assert.NotNil(t, d)
	assert.Equal(t, DeployName, d.LoaderName())
	assert.Equal(t, appName, d.appName)
	assert.Equal(t, ctx, d.ctx)
}

func TestDeployLoader_newPodAgent(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.PodAgent)
	}
	clearEnv()
	defer clearEnv()

	ctx := context.Background()
	appName := "test_app"
	d := NewDeployLoader(ctx, appName).(*DeployLoader)

	d.newPodAgent()
	assert.NotNil(t, d.podAgent)

	d.podAgent = nil
	err := os.Setenv(env.PodAgent, "http://test")
	require.NoError(t, err)
	d.newPodAgent()
	assert.NotNil(t, d.podAgent)
}

func TestDeployLoader_Load(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.PodAgent)
	}
	clearEnv()
	defer clearEnv()

	ctx := context.Background()
	appName := "test_app"
	d := NewDeployLoader(ctx, appName)
	body := []byte(fmt.Sprintf(testData1, "false"))

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		t.Log("Got request", r.URL)
		w.Header()["Content-Type"] = []string{"application/json"}
		w.WriteHeader(http.StatusOK)
		_, err := w.Write(body)
		require.NoError(t, err)
	}))

	err := os.Setenv(env.PodAgent, ts.URL)
	require.NoError(t, err)

	values, err := d.Load()
	assert.NoError(t, err)
	assert.NotNil(t, values)
	assert.Equal(t, uint64(3333), values.MemoryBytesGuarantee)
	assert.Equal(t, uint64(555), values.CPUMilsGuarantee)
	assert.Equal(t, uint64(1), values.CPUCoresGuarantee)
	assert.Equal(t, uint64(4444), values.MemoryBytesLimit)
	assert.Equal(t, uint64(2222), values.CPUMilsLimit)
	assert.Equal(t, uint64(2), values.CPUCoresLimit)
	assert.Equal(t, filepath.Join(DeployConfigPath, appName), values.ConfigPath)
	assert.Equal(t, filepath.Join(DeployLogPath, appName), values.LogPath)
	assert.Equal(t, filepath.Join(DeployLogPath, appName, fmt.Sprintf(DeployLogFile, appName)), values.LogFile)
	assert.Equal(t, DeployRootPath, values.RootPath)
	assert.Equal(t, filepath.Join(DeployPersistentDataPath, appName), values.PersistentDataPath)
	assert.Equal(t, DeployTmpPath, values.TmpPath)
	assert.Equal(t, DeployDataGetterPath, values.DataGetterPath)
	assert.Equal(t, "sas", values.DC)
	assert.Equal(t, "testing", values.ENV)
	assert.Equal(t, DeployDefaultHost, values.ListenHost)
	assert.Equal(t, DeployWebPort, values.ListenWebPort)
	assert.Equal(t, DeployWebSSLPort, values.ListenWebSSLPort)
	assert.Equal(t, DeployAppPort, values.ListenAppPort)
	assert.Equal(t, DeployGRPCPort, values.ListenGRPCPort)
	assert.Equal(t, DeployGRPCSSLPort, values.ListenGRPCSSLPort)
	assert.Equal(t, DeployGRPCAppPort, values.ListenGRPCAppPort)
	assert.Equal(t, DeployDebugPort, values.ListenDebugPort)
	assert.Equal(t, map[string]Volume{}, values.Volumes)
	require.NotNil(t, values.Labels)
	assert.Equal(t, "testing", values.Labels["market_env"])
	assert.Equal(t, "1", values.Labels["test_label"])
	assert.Equal(t, "1", values.Labels["test_label_2"])
	assert.Equal(t, "true", values.Labels["test_label_3"])

	body = []byte(fmt.Sprintf(testData1, "true"))
	httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		t.Log("Got request", r.URL)
		w.Header()["Content-Type"] = []string{"application/json"}
		w.WriteHeader(http.StatusOK)
		_, err := w.Write(body)
		require.NoError(t, err)
	}))

	values, err = d.Load()
	assert.NoError(t, err)
	assert.NotNil(t, values)
	assert.Equal(t, filepath.Join(DeploySoxRootPath, DeployConfigPath, appName), values.ConfigPath)
	assert.Equal(t, filepath.Join(DeploySoxRootPath, DeployRootPath), values.RootPath)
}

func TestFirstNotZeroValue(t *testing.T) {
	values := []uint64{0, 0, 0, 3, 6, 7}
	value := firstNotZeroValue(values...)

	assert.Equal(t, uint64(3), value)

	values = []uint64{0, 0, 0}
	value = firstNotZeroValue(values...)

	assert.Equal(t, uint64(0), value)
}
