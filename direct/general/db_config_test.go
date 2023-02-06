package dbconfig

import (
	"github.com/bmizerany/assert"
	"testing"
)

/*
	Проверяет, что список реплик правильно парсится
*/
func TestClusterHosts(t *testing.T) {
	result := SplitHosts("sas-dasdadadad40.db.yandex.net ,  vla-fdghwgheterhewher.db.yandex.net,iva-oidoqjfq44t45.db.yandex.net, man-owiqrnqkt424.db.yandex.net")
	expectedResult := []string{"sas-dasdadadad40.db.yandex.net", "vla-fdghwgheterhewher.db.yandex.net", "iva-oidoqjfq44t45.db.yandex.net", "man-owiqrnqkt424.db.yandex.net"}
	assert.Equal(t, result, expectedResult)
}

func TestClusterHostsEmptyString(t *testing.T) {
	result := SplitHosts("")
	expectedResult := make([]string, 0)
	assert.Equal(t, result, expectedResult)
}

func TestClusterHostsOneHost(t *testing.T) {
	result := SplitHosts("sas-dasdadadad40.db.yandex.net")
	expectedResult := []string{"sas-dasdadadad40.db.yandex.net"}
	assert.Equal(t, result, expectedResult)
}
