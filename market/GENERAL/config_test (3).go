package runner

import (
	"a.yandex-team.ru/market/sre/services/remon/internal/flags"
	"context"
	"github.com/heetch/confita"
	"github.com/heetch/confita/backend/env"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"os"
	"path"
	"strings"
	"testing"
)

var testCert = []byte(`-----BEGIN CERTIFICATE-----
MIICEzCCAXygAwIBAgIQMIMChMLGrR+QvmQvpwAU6zANBgkqhkiG9w0BAQsFADAS
MRAwDgYDVQQKEwdBY21lIENvMCAXDTcwMDEwMTAwMDAwMFoYDzIwODQwMTI5MTYw
MDAwWjASMRAwDgYDVQQKEwdBY21lIENvMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB
iQKBgQDuLnQAI3mDgey3VBzWnB2L39JUU4txjeVE6myuDqkM/uGlfjb9SjY1bIw4
iA5sBBZzHi3z0h1YV8QPuxEbi4nW91IJm2gsvvZhIrCHS3l6afab4pZBl2+XsDul
rKBxKKtD1rGxlG4LjncdabFn9gvLZad2bSysqz/qTAUStTvqJQIDAQABo2gwZjAO
BgNVHQ8BAf8EBAMCAqQwEwYDVR0lBAwwCgYIKwYBBQUHAwEwDwYDVR0TAQH/BAUw
AwEB/zAuBgNVHREEJzAlggtleGFtcGxlLmNvbYcEfwAAAYcQAAAAAAAAAAAAAAAA
AAAAATANBgkqhkiG9w0BAQsFAAOBgQCEcetwO59EWk7WiJsG4x8SY+UIAA+flUI9
tyC4lNhbcF2Idq9greZwbYCqTTTr2XiRNSMLCOjKyI7ukPoPjo16ocHj+P3vZGfs
h1fIw3cSS2OolhloGw/XM6RWPWtPAlGykKLciQrBru5NAPvCMsb/I1DAceTiotQM
fblo6RBxUQ==
-----END CERTIFICATE-----`)
var testKey = []byte(testingKey(`-----BEGIN RSA TESTING KEY-----
MIICXgIBAAKBgQDuLnQAI3mDgey3VBzWnB2L39JUU4txjeVE6myuDqkM/uGlfjb9
SjY1bIw4iA5sBBZzHi3z0h1YV8QPuxEbi4nW91IJm2gsvvZhIrCHS3l6afab4pZB
l2+XsDulrKBxKKtD1rGxlG4LjncdabFn9gvLZad2bSysqz/qTAUStTvqJQIDAQAB
AoGAGRzwwir7XvBOAy5tM/uV6e+Zf6anZzus1s1Y1ClbjbE6HXbnWWF/wbZGOpet
3Zm4vD6MXc7jpTLryzTQIvVdfQbRc6+MUVeLKwZatTXtdZrhu+Jk7hx0nTPy8Jcb
uJqFk541aEw+mMogY/xEcfbWd6IOkp+4xqjlFLBEDytgbIECQQDvH/E6nk+hgN4H
qzzVtxxr397vWrjrIgPbJpQvBsafG7b0dA4AFjwVbFLmQcj2PprIMmPcQrooz8vp
jy4SHEg1AkEA/v13/5M47K9vCxmb8QeD/asydfsgS5TeuNi8DoUBEmiSJwma7FXY
fFUtxuvL7XvjwjN5B30pNEbc6Iuyt7y4MQJBAIt21su4b3sjXNueLKH85Q+phy2U
fQtuUE9txblTu14q3N7gHRZB4ZMhFYyDy8CKrN2cPg/Fvyt0Xlp/DoCzjA0CQQDU
y2ptGsuSmgUtWj3NM9xuwYPm+Z/F84K6+ARYiZ6PYj013sovGKUFfYAqVXVlxtIX
qyUBnu3X9ps8ZfjLZO7BAkEAlT4R5Yl6cGhaJQYZHOde3JEMhNRcVFMO8dJDaFeo
f9Oeos0UUothgiDktdQHxdNEwLjQf7lJJBzV+5OtwswCWA==
-----END RSA TESTING KEY-----`))

func testingKey(s string) string { return strings.ReplaceAll(s, "TESTING KEY", "PRIVATE KEY") }

var testConfig = []byte(`---
yt:
  token: TESTTOKEN
  lock_path: //tmp/tests/remon.lock
collector:
  interval: 15s
  lock_time: 10s
  pool_size: 10
  servers:
    - name: test
      hostname:  127.0.0.1
      ssl: false
    - name: test2
      tags:
        - test_tag
    - name: test3
      ignore_modules:
        - JuggleR
log:
  level: debug
  path: /test/log
`)

var testInvalidConfig = []byte(`---
yt:
  token: TESTTOKEN
  lock_path: //tmp/tests/remon.lock
collector:
  servers:
    - hostname:  127.0.0.1
`)

func TestDefaultLoader(t *testing.T) {
	_, err := defaultLoader("", "")
	assert.Error(t, err)

	dir, err := ioutil.TempDir("", "config")
	require.NoError(t, err)
	defer os.RemoveAll(dir)

	err = ioutil.WriteFile(path.Join(dir, "config.yaml"), []byte(""), 0600)
	require.NoError(t, err)

	err = ioutil.WriteFile(path.Join(dir, "config_file"), []byte(""), 0600)
	require.NoError(t, err)

	loader, err := defaultLoader("", dir)
	assert.NoError(t, err)
	assert.NotNil(t, loader)

	configPaths[0] = dir
	loader, err = defaultLoader("config_file", "")
	assert.NoError(t, err)
	assert.NotNil(t, loader)
}

func TestLoadTLSConfig(t *testing.T) {
	sslConfig := SSLConfig{}
	err := loadTLSConfig(&sslConfig)
	assert.NoError(t, err)
	assert.Nil(t, sslConfig.TLSConfig)

	dir, err := ioutil.TempDir("", "cert")
	require.NoError(t, err)
	defer os.RemoveAll(dir)

	certName := path.Join(dir, "cert.pem")
	keyName := path.Join(dir, "key.pem")

	err = ioutil.WriteFile(certName, testCert, 0600)
	require.NoError(t, err)
	err = ioutil.WriteFile(keyName, testKey, 0600)
	require.NoError(t, err)

	sslConfig.Certificate = certName
	err = loadTLSConfig(&sslConfig)
	assert.Error(t, err)

	sslConfig.Certificate = ""
	sslConfig.Key = keyName
	err = loadTLSConfig(&sslConfig)
	assert.Error(t, err)

	sslConfig.Certificate = certName
	sslConfig.Key = keyName
	err = loadTLSConfig(&sslConfig)
	assert.NoError(t, err)
	assert.NotNil(t, sslConfig.TLSConfig)

	sslConfig.Certificate = certName + "invalid"
	err = loadTLSConfig(&sslConfig)
	assert.Error(t, err)
}

func TestLoadConfig(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	c := DefaultConfig()
	f := new(flags.Flags)

	err := LoadConfig(ctx, &c, f, nil)
	assert.Error(t, err)

	err = os.Setenv("LOG_LEVEL", "DEBUG")
	require.NoError(t, err)
	loader := confita.NewLoader(env.NewBackend())

	c = DefaultConfig()
	err = LoadConfig(ctx, &c, f, loader)
	assert.Error(t, err)
	assert.Equal(t, "DEBUG", c.LOG.Level)

	dir, err := ioutil.TempDir("", "config")
	require.NoError(t, err)
	defer os.RemoveAll(dir)

	err = ioutil.WriteFile(path.Join(dir, "token"), []byte("TESTTOKEN"), 0600)
	require.NoError(t, err)

	err = os.Setenv("YT_TOKEN_PATH", path.Join(dir, "token"))
	require.NoError(t, err)
	c = DefaultConfig()
	err = LoadConfig(ctx, &c, f, loader)
	assert.Error(t, err)
	assert.Equal(t, "DEBUG", c.LOG.Level)
	assert.Equal(t, "TESTTOKEN", c.YT.Token)

	err = os.Unsetenv("YT_TOKEN_PATH")
	require.NoError(t, err)
	err = os.Unsetenv("LOG_LEVEL")
	require.NoError(t, err)

	configPaths[0] = dir

	err = ioutil.WriteFile(path.Join(dir, "config.yaml"), testInvalidConfig, 0600)
	require.NoError(t, err)
	c = DefaultConfig()
	err = LoadConfig(ctx, &c, f, nil)
	assert.Error(t, err)
	assert.Equal(t, "TESTTOKEN", c.YT.Token)

	err = ioutil.WriteFile(path.Join(dir, "config.yaml"), []byte(""), 0600)
	require.NoError(t, err)
	c = DefaultConfig()
	err = LoadConfig(ctx, &c, f, nil)
	assert.Error(t, err)
	assert.NotNil(t, c.YT)
	assert.NotNil(t, c.SSL)
	assert.NotNil(t, c.Collector)
	assert.NotNil(t, c.LOG)

	err = ioutil.WriteFile(path.Join(dir, "config.yaml"), testConfig, 0600)
	require.NoError(t, err)
	c = DefaultConfig()
	f.LogPath = "/test/ne/log"
	err = LoadConfig(ctx, &c, f, nil)
	assert.NoError(t, err)
	assert.Equal(t, f.LogPath, c.LOG.Path)
	assert.Equal(t, "debug", c.LOG.Level)

	assert.Equal(t, "test", c.Collector.Servers[0].Name)
	assert.Equal(t, "127.0.0.1", c.Collector.Servers[0].HostName)
	assert.Equal(t, uint16(20984), c.Collector.Servers[0].Port)
	assert.Equal(t, false, *c.Collector.Servers[0].SSL)
	assert.Equal(t, []string(nil), c.Collector.Servers[0].Tags)

	assert.Equal(t, "test2", c.Collector.Servers[1].Name)
	assert.Equal(t, "test2", c.Collector.Servers[1].HostName)
	assert.Equal(t, true, *c.Collector.Servers[1].SSL)
	assert.Equal(t, []string{"test_tag"}, c.Collector.Servers[1].Tags)

	assert.Equal(t, true, c.Collector.Servers[2].IgnoreModulesMap["juggler"])
}
