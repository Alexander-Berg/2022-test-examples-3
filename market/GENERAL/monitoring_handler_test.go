package api

import (
	"fmt"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestMonitoring(t *testing.T) {
	a := new(API)
	a.Echo = echo.New()

	req := httptest.NewRequest(http.MethodGet, "/monitoring", nil)
	rec := httptest.NewRecorder()
	c := a.Echo.NewContext(req, rec)

	assert.NoError(t, Monitoring(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, "0;OK", rec.Body.String())
}

func TestMonitoringCertificate(t *testing.T) {
	a := new(API)
	a.Echo = echo.New()
	a.Config = &Config{}

	req := httptest.NewRequest(http.MethodGet, "/monitoring/certificate", nil)
	rec := httptest.NewRecorder()
	c := a.Echo.NewContext(req, rec)

	assert.NoError(t, MonitoringCertificate(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, "0;OK", rec.Body.String())

	expire := 9
	expireTime := time.Now().Add(time.Hour * 24 * 10)
	a.Config.clientCertificateExpireAt = &expireTime

	description := func(format, desk string, expire int) string {
		// Date can change during test run...
		if newDesk := fmt.Sprintf(format, expire-1); desk == newDesk {
			return newDesk
		}
		return fmt.Sprintf(format, expire)
	}

	a.Config.ClientExpireDays = 5
	req = httptest.NewRequest(http.MethodGet, "/monitoring/certificate", nil)
	rec = httptest.NewRecorder()
	c = a.Echo.NewContext(req, rec)

	assert.NoError(t, MonitoringCertificate(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, description("0;OK: Remon client certificate expire in %d days", rec.Body.String(), expire), rec.Body.String())

	a.Config.ClientExpireDays = 15
	req = httptest.NewRequest(http.MethodGet, "/monitoring/certificate", nil)
	rec = httptest.NewRecorder()
	c = a.Echo.NewContext(req, rec)

	assert.NoError(t, MonitoringCertificate(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, description("2;CRIT: Remon client certificate expire in %d days. How-To: https://nda.ya.ru/t/hTdwCCYD4VVzuh", rec.Body.String(), expire), rec.Body.String())
}
