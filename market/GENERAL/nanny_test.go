package loader

import (
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

var dumpJSONTestData1 = `
{
  "@class" : "ru.yandex.iss.Instance",
  "slot" : "1234@sas1-1111.search.yandex.net",
  "configurationId" : "testing_market_mcrp_request_vla#testing_market_mcrp_request_vla-1605101484805",
  "targetState" : "ACTIVE",
  "targetStateOperationId" : "6393fbb0-2423-11eb-a502-59b85fc945ee",
  "transitionTimestamp" : 1605101997803,
  "properties" : {
    "BSCONFIG_IHOST" : "sas1-1111",
    "BSCONFIG_INAME" : "sas1-1111:1234",
    "BSCONFIG_IPORT" : "1234",
    "BSCONFIG_ITAGS" : "SAS_MARKET_TEST_APP a_ctype_testing a_dc_sas a_geo_sas a_itype_marketapp a_line_sas-01 a_metaprj_market a_prj_market a_tier_none cgset_memory_recharge_on_pgfault_1 use_hq_spec enable_hq_report enable_hq_poll",
    "BSCONFIG_SHARDDIR" : "./",
    "BSCONFIG_SHARDNAME" : "",
	"DEPLOY_ENGINE" : "%s",
    "HOSTNAME" : "sas1-1111-abc-sas-market-test-d-efg-1234.gencfg-c.yandex.net",
    "tags" : "SAS_MARKET_TEST_APP a_ctype_testing a_dc_sas a_geo_sas a_itype_marketmcrprequest a_line_sas-01 a_metaprj_market a_prj_market a_tier_none cgset_memory_recharge_on_pgfault_1 use_hq_spec enable_hq_report enable_hq_poll"
  },
  "dynamicProperties" : {
    "BACKBONE_IP_ADDRESS" : "::abcd:fade",
    "GENCFG_GROUP" : "SAS_MARKET_TEST_APP",
    "GENCFG_RELEASE" : "stable-123-r1111",
    "HBF_NAT" : "disabled",
    "NANNY_SNAPSHOT_ID" : "95b9efee62a44840b4aa8c06ba2e7a8e95",
    "SKYNET_SSH" : "enabled"
  },
  "container" : {
    "withDynamicProperties" : false,
    "constraints" : {
      "cpu_guarantee" : "0.555555c",
      "memory_guarantee" : "2222",
      "meta.enable_porto" : "isolate",
      "meta.etc_hosts" : "::1          localhost localhost.localdomain localhost6 localhost6.localdomain6\n127.0.0.1    localhost localhost.localdomain localhost4 localhost4.localdomain4\n::abcd:defa    sas1-1111-abc-sas-market-test-d-efg-1234.gencfg-c.yandex.net\n",
      "meta.hostname" : "sas1-1111-abc-sas-market-test-d-efg-1234.gencfg-c.yandex.net",
      "net_guarantee" : "default: 0",
      "slot.cpu_limit" : "3.3",
      "slot.cpu_policy" : "normal",
      "slot.hostname" : "sas1-1111-abc-sas-market-test-d-efg-1234.gencfg-c.yandex.net",
      "slot.io_policy" : "normal",
      "slot.ip" : "veth ::abcd:defa; veth ::abcd:fade",
      "slot.memory_limit" : "4444",
      "slot.net" : "L3 veth",
      "slot.net_limit" : "default: 0",
      "slot.oom_is_fatal" : "false",
      "slot.ulimit" : "data: 68719476736 68719476736; memlock: 68719476736 68719476736"
    }
  },
  "volumes" : [ {
    "@class" : "ru.yandex.iss.LayeredVolume",
    "mountPoint" : "/",
    "storage" : "/place",
    "properties" : {
      "bind" : "/usr/local/yasmagent /usr/local/yasmagent ro"
    },
    "quota" : "2147483648",
    "quotaCwd" : "2147483648",
    "layers" : [ {
      "@class" : "ru.yandex.iss.Resource",
      "uuid" : "layer_rbtorrent_7d7400ed36694192801226d25110d3f17d7400fa",
      "trafficClass" : {
        "downloadSpeedLimit" : 0
      },
      "verification" : {
        "checksum" : "EMPTY:",
        "checkPeriod" : "0d0h0m"
      },
      "urls" : [ "rbtorrent:7d7400ed36694192801226d25110d3f17d7400fa" ],
      "size" : 0,
      "cached" : false,
      "storage" : "/place"
    }, {
      "@class" : "ru.yandex.iss.Resource",
      "uuid" : "layer_rbtorrent_04bd534adb4f452cba7f072fcc02a73f04bd534a",
      "trafficClass" : {
        "downloadSpeedLimit" : 0
      },
      "verification" : {
        "checksum" : "EMPTY:",
        "checkPeriod" : "0d0h0m"
      },
      "urls" : [ "rbtorrent:04bd534adb4f452cba7f072fcc02a73f04bd534a" ],
      "size" : 0,
      "cached" : false,
      "storage" : "/place"
    } ],
    "shared" : false,
    "uuidIfSet" : {
      "present" : false
    },
    "rootVolume" : true
  }, {
    "@class" : "ru.yandex.iss.LayeredVolume",
    "mountPoint" : "/cores",
    "uuid" : "2d4994e31a30",
    "storage" : "/place",
    "quota" : "6442450944",
    "quotaCwd" : "6442450944",
    "shared" : true,
    "uuidIfSet" : {
      "present" : true
    },
    "rootVolume" : false
  }, {
    "@class" : "ru.yandex.iss.LayeredVolume",
    "mountPoint" : "/logs",
    "uuid" : "4e968e6ea6dd",
    "storage" : "/place",
    "quota" : "42949672960",
    "quotaCwd" : "42949672960",
    "shared" : true,
    "uuidIfSet" : {
      "present" : true
    },
    "rootVolume" : false
  } ],
  "storage" : "/place"
}
`

func createTestDumpJSON() (dir string, callback func(), err error) {
	dir, err = ioutil.TempDir("", "app")
	if err != nil {
		return
	}
	callback = func() {
		_ = os.RemoveAll(dir)
	}
	err = ioutil.WriteFile(filepath.Join(dir, DumpJSONName), []byte(""), 0600)
	return
}

func TestIsNanny(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.DumpJSON)
	}
	clearEnv()
	defer clearEnv()

	dir, remove, err := createTestDumpJSON()
	if remove != nil {
		defer remove()
	}
	require.NoError(t, err)

	isNanny, err := IsNanny()
	require.NoError(t, err)
	assert.False(t, isNanny)

	err = os.Setenv(env.DumpJSON, filepath.Join(dir, DumpJSONName))
	require.NoError(t, err)

	isNanny, err = IsNanny()
	require.NoError(t, err)
	assert.True(t, isNanny)

	err = os.Unsetenv(env.DumpJSON)
	require.NoError(t, err)

	wd, err := os.Getwd()
	require.NoError(t, err)
	err = os.Chdir(dir)
	require.NoError(t, err)
	defer func() { _ = os.Chdir(wd) }()

	isNanny, err = IsNanny()
	require.NoError(t, err)
	assert.True(t, isNanny)
}

func TestNannyProperties_GetTags(t *testing.T) {
	props := NannyProperties{Tags: "a_test_pr    b_test_c a  a____   c_C_c  a_test_test_test"}
	tags := props.GetTags()
	props.Tags = "aaa"
	tags2 := props.GetTags()

	expectedTags := map[string]string{"a_": "__", "a_test": "test_test"}

	assert.Equal(t, expectedTags, tags)
	assert.Equal(t, expectedTags, tags2)

	props = NannyProperties{Tags: ""}
	tags = props.GetTags()

	assert.Equal(t, map[string]string{}, tags)
}

func TestParseCpuCores(t *testing.T) {
	_, err := parseCPUCores("aaaccccccccc")
	assert.Error(t, err)

	_, err = parseMemoryBytes(strings.Repeat("1", 64) + ".0")
	assert.Error(t, err)

	_, err = parseMemoryBytes("0." + strings.Repeat("1", 64))
	assert.Error(t, err)

	v, err := parseCPUCores("111110000cccc")
	assert.NoError(t, err)
	assert.Equal(t, uint64(111110000000), v)

	v, err = parseCPUCores("10.001002")
	assert.NoError(t, err)
	assert.Equal(t, uint64(10001), v)
}

func TestParseMemoryBytes(t *testing.T) {
	_, err := parseMemoryBytes("414141aaaccccccccc")
	assert.Error(t, err)

	_, err = parseMemoryBytes("10.0")
	assert.Error(t, err)

	_, err = parseMemoryBytes(strings.Repeat("1", 64))
	assert.Error(t, err)

	v, err := parseMemoryBytes("1111111")
	assert.NoError(t, err)
	assert.Equal(t, uint64(1111111), v)
}

func TestAppendVolumes(t *testing.T) {
	err := appendVolumes(map[string]Volume{}, []*NannyVolume{{Class: NannyLayeredVolumeClass, MountPoint: "/", Quota: "aaaaaa"}})
	assert.Error(t, err)

	v := map[string]Volume{}
	err = appendVolumes(v, []*NannyVolume{
		{Class: NannyLayeredVolumeClass, MountPoint: "/aaaa", Quota: "1", RootVolume: true},
		{Class: NannyLayeredVolumeClass, MountPoint: "/vol", Quota: "2", RootVolume: false},
		{Class: "", MountPoint: "/notavol", Quota: "3", RootVolume: false},
	})
	assert.NoError(t, err)
	assert.Equal(t, map[string]Volume{
		"/":    {MountPoint: "/", Quota: uint64(1)},
		"/vol": {MountPoint: "/vol", Quota: uint64(2)},
	}, v)
}

func TestBsConfigPort(t *testing.T) {
	_, err := parseMemoryBytes("414141aaaccccccccc")
	assert.Error(t, err)

	_, err = parseMemoryBytes("10.0")
	assert.Error(t, err)

	_, err = parseMemoryBytes(strings.Repeat("1", 64))
	assert.Error(t, err)

	v, err := parseMemoryBytes("1111111")
	assert.NoError(t, err)
	assert.Equal(t, uint64(1111111), v)
}

func TestBsConfigIPort(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv("BSCONFIG_IPORT")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_5")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_8")
	}
	clearEnv()
	defer clearEnv()

	_ = os.Setenv("BSCONFIG_IPORT", "")
	_, err := bsConfigIPort(0)
	assert.Error(t, err)

	_ = os.Setenv("BSCONFIG_IPORT", "1")
	p, err := bsConfigIPort(0)
	assert.NoError(t, err)
	assert.Equal(t, uint16(1), p)

	_, err = bsConfigIPort(5)
	assert.Error(t, err)

	_ = os.Setenv("BSCONFIG_IPORT_PLUS_5", "1")
	p, err = bsConfigIPort(5)
	assert.NoError(t, err)
	assert.Equal(t, uint16(1), p)

	_ = os.Setenv("BSCONFIG_IPORT_PLUS_8", strings.Repeat("1", 6))
	_, err = bsConfigIPort(8)
	assert.Error(t, err)
}

func TestNewNannyLoader(t *testing.T) {
	ctx := context.Background()

	appName := "test_app"
	d := NewNannyLoader(ctx, appName).(*NannyLoader)

	assert.NotNil(t, d)
	assert.Equal(t, NannyName, d.LoaderName())
	assert.Equal(t, appName, d.appName)
	assert.Equal(t, ctx, d.ctx)
}

func TestNannyContainerRoot(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.ContainerRoot)
	}
	clearEnv()
	defer clearEnv()

	assert.Equal(t, NannyContainerRoot, nannyContainerRoot())

	_ = os.Setenv(env.ContainerRoot, "/test")
	assert.Equal(t, "/test", nannyContainerRoot())
}

func TestNannyLoader_Load(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.ContainerRoot)
		_ = os.Unsetenv(env.DumpJSON)
		_ = os.Unsetenv("BSCONFIG_IPORT")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_1")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_2")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_3")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_4")
		_ = os.Unsetenv("BSCONFIG_IPORT_PLUS_20")
	}
	clearEnv()
	defer clearEnv()

	ctx := context.Background()
	appName := "test_app"
	n := NewNannyLoader(ctx, appName).(*NannyLoader)

	nannyDir, err := ioutil.TempDir("", "nanny")
	require.NoError(t, err)

	err = os.MkdirAll(filepath.Join(nannyDir, NannyLogPath), 0700)
	require.NoError(t, err)
	err = os.Setenv(env.ContainerRoot, nannyDir)
	require.NoError(t, err)

	dir, remove, err := createTestDumpJSON()
	if remove != nil {
		defer remove()
	}
	require.NoError(t, err)

	err = ioutil.WriteFile(filepath.Join(dir, DumpJSONName), []byte(fmt.Sprintf(dumpJSONTestData1, "None")), 0600)
	require.NoError(t, err)

	err = os.Setenv(env.DumpJSON, filepath.Join(dir, DumpJSONName))
	require.NoError(t, err)

	wd, err := os.Getwd()
	require.NoError(t, err)

	_ = os.Setenv("BSCONFIG_IPORT", "1")
	_ = os.Setenv("BSCONFIG_IPORT_PLUS_1", "2")
	_ = os.Setenv("BSCONFIG_IPORT_PLUS_2", "3")
	_ = os.Setenv("BSCONFIG_IPORT_PLUS_3", "4")
	_ = os.Setenv("BSCONFIG_IPORT_PLUS_4", "5")
	_ = os.Setenv("BSCONFIG_IPORT_PLUS_20", "21")

	values, err := n.Load()
	require.NoError(t, err)
	assert.NotNil(t, values)
	assert.Equal(t, uint64(2222), values.MemoryBytesGuarantee)
	assert.Equal(t, uint64(555), values.CPUMilsGuarantee)
	assert.Equal(t, uint64(1), values.CPUCoresGuarantee)
	assert.Equal(t, uint64(4444), values.MemoryBytesLimit)
	assert.Equal(t, uint64(3300), values.CPUMilsLimit)
	assert.Equal(t, uint64(3), values.CPUCoresLimit)
	assert.Equal(t, filepath.Join(wd, NannyRelativeConfPath, appName), values.ConfigPath)
	assert.Equal(t, filepath.Join(nannyDir, NannyLogPath, appName), values.LogPath)
	assert.Equal(t, filepath.Join(nannyDir, NannyLogPath, appName, fmt.Sprintf(DeployLogFile, appName)), values.LogFile)
	assert.Equal(t, wd, values.RootPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativePersistentDataPath, appName), values.PersistentDataPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativeTmpPath), values.TmpPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativeDataGetterPath), values.DataGetterPath)
	assert.Equal(t, "sas", values.DC)
	assert.Equal(t, "testing", values.ENV)
	assert.Equal(t, NannyDefaultHost, values.ListenHost)
	assert.Equal(t, uint16(1), values.ListenWebPort)
	assert.Equal(t, uint16(1), values.ListenWebSSLPort)
	assert.Equal(t, uint16(2), values.ListenAppPort)
	assert.Equal(t, uint16(3), values.ListenGRPCPort)
	assert.Equal(t, uint16(4), values.ListenGRPCSSLPort)
	assert.Equal(t, uint16(5), values.ListenGRPCAppPort)
	assert.Equal(t, uint16(21), values.ListenDebugPort)
	assert.Equal(t, map[string]Volume{
		"/":      {MountPoint: "/", Quota: 2147483648},
		"/cores": {MountPoint: "/cores", Quota: 6442450944},
		"/logs":  {MountPoint: "/logs", Quota: 42949672960},
	}, values.Volumes)

	err = ioutil.WriteFile(filepath.Join(dir, DumpJSONName), []byte(fmt.Sprintf(dumpJSONTestData1, "YP_LITE")), 0600)
	require.NoError(t, err)

	_ = os.Setenv(NannyYPLiteCPUGuarantee, "4.444c")
	_ = os.Setenv(NannyYPLiteCPULimit, "5.555")
	_ = os.Setenv(NannyYPLiteMemoryGuarantee, "666")
	_ = os.Setenv(NannyYPLiteMemoryLimit, "777")

	values, err = n.Load()
	require.NoError(t, err)
	assert.NotNil(t, values)
	assert.Equal(t, uint64(666), values.MemoryBytesGuarantee)
	assert.Equal(t, uint64(4444), values.CPUMilsGuarantee)
	assert.Equal(t, uint64(4), values.CPUCoresGuarantee)
	assert.Equal(t, uint64(777), values.MemoryBytesLimit)
	assert.Equal(t, uint64(5555), values.CPUMilsLimit)
	assert.Equal(t, uint64(5), values.CPUCoresLimit)
	assert.Equal(t, filepath.Join(wd, NannyRelativeConfPath, appName), values.ConfigPath)
	assert.Equal(t, filepath.Join(nannyDir, NannyLogPath, appName), values.LogPath)
	assert.Equal(t, filepath.Join(nannyDir, NannyLogPath, appName, fmt.Sprintf(DeployLogFile, appName)), values.LogFile)
	assert.Equal(t, wd, values.RootPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativePersistentDataPath, appName), values.PersistentDataPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativeTmpPath), values.TmpPath)
	assert.Equal(t, filepath.Join(wd, NannyRelativeDataGetterPath), values.DataGetterPath)
	assert.Equal(t, "sas", values.DC)
	assert.Equal(t, "testing", values.ENV)
	assert.Equal(t, NannyDefaultHost, values.ListenHost)
	assert.Equal(t, uint16(1), values.ListenWebPort)
	assert.Equal(t, uint16(1), values.ListenWebSSLPort)
	assert.Equal(t, uint16(2), values.ListenAppPort)
	assert.Equal(t, uint16(3), values.ListenGRPCPort)
	assert.Equal(t, uint16(4), values.ListenGRPCSSLPort)
	assert.Equal(t, uint16(5), values.ListenGRPCAppPort)
	assert.Equal(t, uint16(21), values.ListenDebugPort)
	assert.Equal(t, map[string]Volume{
		"/":      {MountPoint: "/", Quota: 2147483648},
		"/cores": {MountPoint: "/cores", Quota: 6442450944},
		"/logs":  {MountPoint: "/logs", Quota: 42949672960},
	}, values.Volumes)
	assert.NotNil(t, values.Labels)
}
