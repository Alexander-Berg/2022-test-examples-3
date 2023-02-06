package database

import (
	"context"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"golang.yandex/hasql"
	"gorm.io/gorm"

	"a.yandex-team.ru/travel/avia/weekendtour/internal/models"
	"a.yandex-team.ru/travel/avia/weekendtour/internal/pgclient"
)

func getPgEnv(key, defaultValue string) string {
	if value := os.Getenv("PG_LOCAL_" + strings.ToUpper(key)); value == "" {
		return defaultValue
	} else {
		return value
	}
}

func GetTestPgClient() *pgclient.PGClient {
	port, err := strconv.Atoi(getPgEnv("port", "5432"))
	if err != nil {
		panic(err)
	}
	client, err := pgclient.NewPGClient(
		[]string{"localhost"},
		port,
		getPgEnv("database", "postgres"),
		getPgEnv("user", "postgres"),
		getPgEnv("password", "postgres"),
		pgclient.DefaultInitTimeout,
		append(pgclient.DefaultClusterOptions, hasql.WithUpdateTimeout(2*time.Second)),
	)
	if err != nil {
		panic(err)
	}
	return client
}

func cleanUp(testPgClient *pgclient.PGClient) {
	ctx, cancelFunc := context.WithTimeout(context.Background(), GetDBTestTimeout())
	defer cancelFunc()
	err := testPgClient.ExecuteInTransaction(
		hasql.Primary, func(db *gorm.DB) error {
			db = db.Session(&gorm.Session{Context: ctx, AllowGlobalUpdate: true})
			return db.Delete(&models.WeekendTour{}).Error
		},
	)
	if err != nil {
		panic(err)
	}
}

func NewDBCleaner(testPgClient *pgclient.PGClient) func(testFunc func(*testing.T)) func(*testing.T) {
	return func(testFunc func(*testing.T)) func(*testing.T) {
		return func(t *testing.T) {
			if err := models.DropAll(testPgClient); err != nil {
				panic(err)
			}
			if err := models.MigrateAndInit(testPgClient); err != nil {
				panic(err)
			}
			cleanUp(testPgClient)
			t.Cleanup(func() { cleanUp(testPgClient) })
			testFunc(t)
		}
	}
}

func GetDBTestTimeout() time.Duration {
	if yaTest := os.Getenv("YA_TEST_RUNNER"); yaTest != "" {
		return 10 * time.Second
	}
	return 5 * time.Minute
}
