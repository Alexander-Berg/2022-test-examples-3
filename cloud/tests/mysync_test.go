package tests

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"text/template"
	"time"

	"github.com/DATA-DOG/godog"
	"github.com/DATA-DOG/godog/gherkin"
	"github.com/go-sql-driver/mysql"
	"github.com/go-zookeeper/zk"
	"github.com/jmoiron/sqlx"

	"a.yandex-team.ru/cloud/mdb/internal/dcs"
	"a.yandex-team.ru/cloud/mdb/internal/testutil"
	"a.yandex-team.ru/cloud/mdb/mysync/internal/config"
	mysql_internal "a.yandex-team.ru/cloud/mdb/mysync/internal/mysql"
)

//"database/sql"

const (
	yes                        = "Yes"
	zkName                     = "zoo"
	zkPort                     = 2181
	zkConnectTimeout           = 1 * time.Minute
	mysqlName                  = "mysql"
	mysqlPort                  = 3306
	mysqlAdminUser             = "admin"
	mysqlAdminPassword         = "admin_pwd"
	mysqlOrdinaryUser          = "user"
	mysqlOrdinaryPassword      = "user_pwd"
	mysqlConnectTimeout        = 30 * time.Second
	mysqlInitialConnectTimeout = 2 * time.Minute
	mysqlQueryTimeout          = 2 * time.Second
)

var mysqlLogsToSave = map[string]string{
	"/var/log/supervisor.log":  "supervisor.log",
	"/var/log/mysync.log":      "mysync.log",
	"/var/log/mysql/error.log": "mysqld.log",
	"/var/log/mysql/query.log": "query.log",
	"/etc/mysync.yaml":         "mysync.yaml",
}

var zkLogsToSave = map[string]string{
	"/var/log/zookeeper/zookeeper--server-%s.log": "zookeeper.log",
}

type noLogger struct{}

func (noLogger) Printf(string, ...interface{}) {}

func (noLogger) Print(...interface{}) {}

type testContext struct {
	variables         map[string]interface{}
	templateErr       error
	composer          testutil.Composer
	composerEnv       []string
	zk                *zk.Conn
	dbs               map[string]*sqlx.DB
	zkQueryResult     string
	sqlQueryResult    []map[string]interface{}
	sqlUserQueryError sync.Map // host -> error
	commandRetcode    int
	commandOutput     string
}

func newTestContext() (*testContext, error) {
	var err error
	tctx := new(testContext)
	tctx.composer, err = testutil.NewDockerComposer("mysync", "images/docker-compose.yaml")
	if err != nil {
		return nil, err
	}
	tctx.dbs = make(map[string]*sqlx.DB)
	return tctx, nil
}

func (tctx *testContext) saveLogs(scenario string) error {
	for _, service := range tctx.composer.Services() {
		var logsToSave map[string]string
		switch {
		case strings.HasPrefix(service, mysqlName):
			logsToSave = mysqlLogsToSave
		case strings.HasPrefix(service, zkName):
			logsToSave = zkLogsToSave
		default:
			continue
		}
		logdir := filepath.Join("logs", scenario, service)
		err := os.MkdirAll(logdir, 0755)
		if err != nil {
			return err
		}
		for remotePath, localPath := range logsToSave {
			if strings.Contains(remotePath, "%") {
				remotePath = fmt.Sprintf(remotePath, service)
			}
			remoteFile, err := tctx.composer.GetFile(service, remotePath)
			if err != nil {
				return err
			}
			defer func() { _ = remoteFile.Close() }()
			localFile, err := os.OpenFile(filepath.Join(logdir, localPath), os.O_RDWR|os.O_CREATE, 0644)
			if err != nil {
				return err
			}
			defer func() { _ = localFile.Close() }()
			_, err = io.Copy(localFile, remoteFile)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (tctx *testContext) templateStep(step *gherkin.Step) error {
	var err error
	step.Text, err = tctx.templateString(step.Text)
	if err != nil {
		return err
	}

	if v, ok := step.Argument.(*gherkin.DocString); ok {
		v.Content, err = tctx.templateString(v.Content)
		if err != nil {
			return err
		}
		step.Argument = v
	}
	if v, ok := step.Argument.(*gherkin.DataTable); ok {
		if v.Rows != nil {
			for _, row := range v.Rows {
				for _, cell := range row.Cells {
					cell.Value, err = tctx.templateString(cell.Value)
					if err != nil {
						return err
					}
				}
			}
		}
	}
	return nil
}

func (tctx *testContext) templateString(data string) (string, error) {
	if !strings.Contains(data, "{{") {
		return data, nil
	}
	tpl, err := template.New(data).Parse(data)
	if err != nil {
		return data, err
	}
	var res strings.Builder
	err = tpl.Execute(&res, tctx.variables)
	if err != nil {
		return data, err
	}
	return res.String(), nil
}

func (tctx *testContext) cleanup() {
	if tctx.zk != nil {
		tctx.zk.Close()
		tctx.zk = nil
	}
	for _, db := range tctx.dbs {
		if err := db.Close(); err != nil {
			log.Printf("failed to close db connection: %s", err)
		}
	}
	tctx.dbs = make(map[string]*sqlx.DB)
	if err := tctx.composer.Down(); err != nil {
		log.Printf("failed to tear down compose: %s", err)
	}

	tctx.variables = make(map[string]interface{})
	tctx.composerEnv = make([]string, 0)
	tctx.zkQueryResult = ""
	tctx.sqlQueryResult = make([]map[string]interface{}, 0)
	tctx.sqlUserQueryError = sync.Map{}
	tctx.commandRetcode = 0
	tctx.commandOutput = ""
}

func (tctx *testContext) connectZookeeper(addrs []string, timeout time.Duration) (*zk.Conn, error) {
	conn, ec, err := zk.Connect(addrs, time.Second, zk.WithLogger(noLogger{}))
	if err != nil {
		return nil, err
	}
	go func() {
		for range ec {
		}
	}()
	testutil.Retry(func() bool {
		_, _, err = conn.Get("/")
		return err == nil
	}, timeout, time.Second)
	if err != nil {
		return nil, fmt.Errorf("failed to ping zookeeper within %s: %s", timeout, err)
	}
	return conn, nil
}

// nolint: unparam
func (tctx *testContext) connectMysql(addr string, timeout time.Duration) (*sqlx.DB, error) {
	return tctx.connectMysqlWithCredentials(mysqlAdminUser, mysqlAdminPassword, addr, timeout)
}

func (tctx *testContext) connectMysqlWithCredentials(username string, password string, addr string, timeout time.Duration) (*sqlx.DB, error) {
	_ = mysql.SetLogger(noLogger{})
	connTimeout := 2 * time.Second
	dsn := fmt.Sprintf("%s:%s@tcp(%s)/mysql?timeout=%s", username, password, addr, connTimeout)
	db, err := sqlx.Open("mysql", dsn)
	if err != nil {
		return nil, err
	}
	// sql is lazy in go, so we need ping db
	testutil.Retry(func() bool {
		//var rows *sql.Rows
		ctx, cancel := context.WithTimeout(context.Background(), 4*time.Second)
		defer cancel()
		err = db.PingContext(ctx)
		return err == nil
	}, timeout, connTimeout)
	if err != nil {
		_ = db.Close()
		return nil, err
	}
	return db, nil
}

func (tctx *testContext) getMysqlConnection(host string) (*sqlx.DB, error) {
	db, ok := tctx.dbs[host]
	if !ok {
		return nil, fmt.Errorf("mysql %s is not in cluster", host)
	}
	err := db.Ping()
	if err == nil {
		return db, nil
	}
	addr, err := tctx.composer.GetAddr(host, mysqlPort)
	if err != nil {
		return nil, fmt.Errorf("failed to get mysql addr %s: %s", host, err)
	}
	db, err = tctx.connectMysql(addr, mysqlConnectTimeout)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to mysql %s: %s", host, err)
	}
	tctx.dbs[host] = db
	return db, nil
}

func (tctx *testContext) queryMysql(host string, query string, args interface{}) ([]map[string]interface{}, error) {
	if args == nil {
		args = struct{}{}
	}
	db, err := tctx.getMysqlConnection(host)
	if err != nil {
		return nil, err
	}

	// sqlx can't execute requests with semicolon
	// we will execute them in single connection
	queries := strings.Split(query, ";")
	var result []map[string]interface{}

	for _, q := range queries {
		tctx.sqlQueryResult = nil
		q = strings.TrimSpace(q)
		if q == "" {
			continue
		}

		result, err = tctx.doMysqlQuery(db, q, args, mysqlQueryTimeout)
		tctx.sqlQueryResult = result
	}

	return result, err
}

func (tctx *testContext) queryMysqlViaConnection(db *sqlx.DB, host string, query string, args interface{}, timeout time.Duration) ([]map[string]interface{}, error) {
	if args == nil {
		args = struct{}{}
	}

	tctx.sqlQueryResult = nil
	tctx.sqlUserQueryError.Delete(host)

	result, err := tctx.doMysqlQuery(db, query, args, timeout)

	tctx.sqlQueryResult = result
	tctx.sqlUserQueryError.Store(host, err)
	return result, err
}

func (tctx *testContext) doMysqlQuery(db *sqlx.DB, query string, args interface{}, timeout time.Duration) ([]map[string]interface{}, error) {
	if args == nil {
		args = struct{}{}
	}
	ctx, cancel := context.WithTimeout(context.Background(), timeout)
	defer cancel()
	rows, err := db.NamedQueryContext(ctx, query, args)
	if err != nil {
		log.Printf("query error %#v\n", err)
		return nil, err
	}
	defer func() {
		_ = rows.Close()
	}()

	result := make([]map[string]interface{}, 0)
	for rows.Next() {
		rowmap := make(map[string]interface{})
		err = rows.MapScan(rowmap)
		if err != nil {
			return nil, err
		}
		for k, v := range rowmap {
			if v2, ok := v.([]byte); ok {
				rowmap[k] = string(v2)
			}
		}
		result = append(result, rowmap)
	}
	return result, nil
}

func (tctx *testContext) stepClusterEnvironmentIs(body *gherkin.DocString) error {
	var env []string
	byLine := func(r rune) bool {
		return r == '\n' || r == '\r'
	}
	for _, e := range strings.FieldsFunc(body.Content, byLine) {
		if e = strings.TrimSpace(e); e != "" {
			env = append(env, e)
		}
	}
	tctx.composerEnv = env
	return nil
}

func (tctx *testContext) stepClusterIsUpAndRunning(createHaNodes bool) error {
	err := tctx.composer.Up(tctx.composerEnv)
	if err != nil {
		return fmt.Errorf("failed to setup compose cluster: %s", err)
	}

	// check zookeepers
	zkAddrs := make([]string, 0)
	for _, service := range tctx.composer.Services() {
		if strings.HasPrefix(service, zkName) {
			addr, err2 := tctx.composer.GetAddr(service, zkPort)
			if err2 != nil {
				return fmt.Errorf("failed to connect to zookeeper %s: %s", service, err2)
			}
			zkAddrs = append(zkAddrs, addr)
		}
	}
	tctx.zk, err = tctx.connectZookeeper(zkAddrs, zkConnectTimeout)
	if err != nil {
		return fmt.Errorf("failed to connect to zookeeper %s: %s", zkAddrs, err)
	}
	if err = tctx.createZookeeperNode("/test"); err != nil {
		return fmt.Errorf("failed to create namespace zk node due %s", err)
	}
	if createHaNodes {
		if err = tctx.createZookeeperNode(dcs.JoinPath("/test", dcs.PathHANodesPrefix)); err != nil {
			return fmt.Errorf("failed to create path prefix zk node due %s", err)
		}
	}

	// check databases
	for _, service := range tctx.composer.Services() {
		if strings.HasPrefix(service, mysqlName) {
			if createHaNodes {
				if err = tctx.createZookeeperNode(dcs.JoinPath("/test", dcs.PathHANodesPrefix, service)); err != nil {
					return fmt.Errorf("failed to create %s zk node due %s", service, err)
				}
			}
			addr, err2 := tctx.composer.GetAddr(service, mysqlPort)
			if err2 != nil {
				return fmt.Errorf("failed to get mysql addr %s: %s", service, err2)
			}
			db, err2 := tctx.connectMysql(addr, mysqlInitialConnectTimeout)
			if err2 != nil {
				return fmt.Errorf("failed to connect to mysql %s: %s", service, err2)
			}
			tctx.dbs[service] = db
		}
	}
	return nil
}

func (tctx *testContext) stepHostIsStopped(host string) error {
	return tctx.composer.Stop(host)
}

func (tctx *testContext) stepHostIsDetachedFromTheNetwork(host string) error {
	return tctx.composer.DetachFromNet(host)
}

func (tctx *testContext) stepHostIsStarted(host string) error {
	return tctx.composer.Start(host)
}

func (tctx *testContext) stepHostIsAttachedToTheNetwork(host string) error {
	return tctx.composer.AttachToNet(host)
}

func (tctx *testContext) stepHostIsAdded(host string) error {
	err := tctx.composer.Start(host)
	if err != nil {
		return err
	}
	return tctx.createZookeeperNode(dcs.JoinPath("/test", dcs.PathHANodesPrefix, host))
}

func (tctx *testContext) stepHostIsDeleted(host string) error {
	err := tctx.composer.Stop(host)
	if err != nil {
		return err
	}
	return tctx.stepIDeleteZookeeperNode(dcs.JoinPath("/test", dcs.PathHANodesPrefix, host))
}

func (tctx *testContext) stepMysqlOnHostKilled(host string) error {
	cmd := "supervisorctl signal KILL mysqld"
	_, _, err := tctx.composer.RunCommand(host, cmd, 10*time.Second)
	return err
}

func (tctx *testContext) stepMysqlOnHostStarted(host string) error {
	cmd := "supervisorctl start mysqld"
	_, _, err := tctx.composer.RunCommand(host, cmd, 10*time.Second)
	return err
}

func (tctx *testContext) stepMysqlOnHostRestarted(host string) error {
	cmd := "supervisorctl restart mysqld"
	_, _, err := tctx.composer.RunCommand(host, cmd, 30*time.Second)
	return err
}

func (tctx *testContext) stepMysqlOnHostStopped(host string) error {
	cmd := "supervisorctl signal TERM mysqld"
	_, _, err := tctx.composer.RunCommand(host, cmd, 10*time.Second)
	return err
}

func (tctx *testContext) stepRunHeavyUserRequests(host string, sleepTime int) error {
	// don't use cached connections:
	addr, err := tctx.composer.GetAddr(host, mysqlPort)
	if err != nil {
		return fmt.Errorf("failed to get mysql addr %s: %s", host, err)
	}
	db, err := tctx.connectMysqlWithCredentials(mysqlOrdinaryUser, mysqlOrdinaryPassword, addr, mysqlConnectTimeout)
	if err != nil {
		return fmt.Errorf("failed to connect to mysql %s: %s", host, err)
	}

	if _, err := tctx.queryMysqlViaConnection(db, host, "CREATE TABLE IF NOT EXISTS t1(i INT)", nil, mysqlQueryTimeout); err != nil {
		return err
	}
	if _, err := tctx.queryMysqlViaConnection(db, host, "TRUNCATE TABLE t1", nil, mysqlQueryTimeout); err != nil {
		return err
	}
	if _, err := tctx.queryMysqlViaConnection(db, host, "INSERT INTO t1 VALUES(1)", nil, mysqlQueryTimeout); err != nil {
		return err
	}

	go func() {
		defer db.Close()
		timeout := time.Duration(sleepTime+2) * time.Second
		_, err := tctx.queryMysqlViaConnection(db, host, "UPDATE t1 SET i = sleep(:sleep_time)", map[string]interface{}{
			"sleep_time": sleepTime,
		}, timeout)
		if err != nil {
			fmt.Printf("error while running heavy requests %v\n", err)
		}
	}()

	return nil
}

func (tctx *testContext) stepRunHeavyReadUserRequests(host string, sleepTime int) error {
	// don't use cached connections:
	addr, err := tctx.composer.GetAddr(host, mysqlPort)
	if err != nil {
		return fmt.Errorf("failed to get mysql addr %s: %s", host, err)
	}
	db, err := tctx.connectMysqlWithCredentials(mysqlOrdinaryUser, mysqlOrdinaryPassword, addr, mysqlConnectTimeout)
	if err != nil {
		return fmt.Errorf("failed to connect to mysql %s: %s", host, err)
	}

	go func() {
		defer db.Close()
		timeout := time.Duration(sleepTime+2) * time.Second
		_, err := tctx.queryMysqlViaConnection(db, host, "SELECT *, sleep(:sleep_time) as time FROM sys.session", map[string]interface{}{
			"sleep_time": sleepTime,
		}, timeout)
		if err != nil {
			fmt.Printf("error while running long read requests %v\n", err)
		}
	}()

	return nil
}

func (tctx *testContext) stepHostShouldHaveFile(node string, path string) error {
	remoteFile, err := tctx.composer.GetFile(node, path)
	if err != nil {
		return err
	}
	err = remoteFile.Close()
	if err != nil {
		return err
	}
	return nil
}

func (tctx *testContext) stepHostShouldHaveFileWithin(node string, path string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepHostShouldHaveFile(node, path)
		return err == nil

	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepIRunCommandOnHost(host string, body *gherkin.DocString) error {
	cmd := strings.TrimSpace(body.Content)
	var err error
	tctx.commandRetcode, tctx.commandOutput, err = tctx.composer.RunCommand(host, cmd, 10*time.Second)
	return err
}

func (tctx *testContext) stepSetUsedSpace(host string, percent int) error {
	if percent < 0 || percent > 100 {
		return fmt.Errorf("incorrect percent value: %d", percent)
	}
	cmd := fmt.Sprintf("echo %d > /tmp/usedspace", percent)
	_, _, err := tctx.composer.RunCommand(host, cmd, 10*time.Second)
	return err
}

func (tctx *testContext) stepIRunAsyncCommandOnHost(host string, body *gherkin.DocString) error {
	cmd := strings.TrimSpace(body.Content)
	return tctx.composer.RunAsyncCommand(host, cmd)
}

func (tctx *testContext) stepIRunCommandOnHostWithTimeout(host string, timeout int, body *gherkin.DocString) error {
	cmd := strings.TrimSpace(body.Content)
	var err error
	tctx.commandRetcode, tctx.commandOutput, err = tctx.composer.RunCommand(host, cmd, time.Duration(timeout)*time.Second)
	return err
}

func (tctx *testContext) stepIRunCommandOnHostUntilResultMatch(host string, pattern string, timeout int, body *gherkin.DocString) error {
	matcher, err := GetMatcher("regexp")
	if err != nil {
		return err
	}

	var lastError error
	testutil.Retry(func() bool {
		cmd := strings.TrimSpace(body.Content)
		tctx.commandRetcode, tctx.commandOutput, lastError = tctx.composer.RunCommand(host, cmd, time.Duration(timeout)*time.Second)
		if lastError != nil {
			return false
		}
		lastError = matcher(tctx.commandOutput, strings.TrimSpace(pattern))
		return lastError == nil

	}, time.Duration(timeout)*time.Second, time.Second)

	return lastError
}

// Make sure zk node is absent, then stop slave, change master & start slave again
func (tctx *testContext) stepIChangeReplicationSourceWithTimeout(host, replicationSource string, timeout int) error {
	if err := tctx.stepIDeleteZookeeperNode(dcs.JoinPath("/test", dcs.PathHANodesPrefix, host)); err != nil {
		return err
	}
	if _, err := tctx.queryMysql(host, "STOP SLAVE", nil); err != nil {
		return err
	}
	query := `CHANGE MASTER TO
								MASTER_HOST = :host ,
								MASTER_PORT = :port ,
								MASTER_USER = :user ,
								MASTER_PASSWORD = :password ,
								MASTER_SSL = :ssl ,
								MASTER_SSL_CA = :sslCa ,
								MASTER_SSL_VERIFY_SERVER_CERT = 1,
								MASTER_AUTO_POSITION = 1,
								MASTER_CONNECT_RETRY = :connectRetry,
								MASTER_RETRY_COUNT = :retryCount,
								MASTER_HEARTBEAT_PERIOD = :heartbeatPeriod `
	dc, err := config.DefaultConfig()
	if err != nil {
		return err
	}
	query = mysql_internal.Mogrify(query, map[string]interface{}{
		"host":            replicationSource,
		"port":            dc.MySQL.Port,
		"user":            dc.MySQL.ReplicationUser,
		"password":        dc.MySQL.ReplicationPassword,
		"ssl":             0,
		"sslCa":           dc.MySQL.ReplicationSslCA,
		"retryCount":      dc.MySQL.ReplicationRetryCount,
		"connectRetry":    dc.MySQL.ReplicationConnectRetry,
		"heartbeatPeriod": dc.MySQL.ReplicationHeartbeatPeriod,
	})
	if _, err := tctx.queryMysql(host, query, nil); err != nil {
		return err
	}
	_, err = tctx.queryMysql(host, "START SLAVE", nil)
	return err
}

func (tctx *testContext) stepCommandReturnCodeShouldBe(code int) error {
	if tctx.commandRetcode != code {
		return fmt.Errorf("command return code is %d, while expected %d\n%s", tctx.commandRetcode, code, tctx.commandOutput)
	}
	return nil
}

func (tctx *testContext) stepCommandOutputShouldMatch(matcher string, body *gherkin.DocString) error {
	m, err := GetMatcher(matcher)
	if err != nil {
		return err
	}
	return m(tctx.commandOutput, strings.TrimSpace(body.Content))
}

func (tctx *testContext) stepIRunSQLOnHost(host string, body *gherkin.DocString) error {
	query := strings.TrimSpace(body.Content)
	_, err := tctx.queryMysql(host, query, struct{}{})
	return err
}

func (tctx *testContext) stepSQLResultShouldMatch(matcher string, body *gherkin.DocString) error {
	m, err := GetMatcher(matcher)
	if err != nil {
		return err
	}
	res, err := json.Marshal(tctx.sqlQueryResult)
	if err != nil {
		panic(err)
	}
	return m(string(res), strings.TrimSpace(body.Content))
}

func (tctx *testContext) stepThereIsSQLErrorWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		_, hasError := tctx.sqlUserQueryError.Load(host)
		if hasError {
			err = nil
			return true // exit
		}
		err = fmt.Errorf("there is no expected sql error")
		return false
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepThereIsNoSQLErrorWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		sqlError, hasError := tctx.sqlUserQueryError.Load(host)
		if hasError {
			err = fmt.Errorf("there is an unexpected sql error %s", sqlError)
			return true // exit
		}
		err = nil
		return false
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepBreakReplicationOnHost(host string) error {
	if _, err := tctx.queryMysql(host, "STOP SLAVE IO_THREAD", struct{}{}); err != nil {
		return err
	}
	if _, err := tctx.queryMysql(host, "STOP SLAVE", struct{}{}); err != nil {
		return err
	}
	if _, err := tctx.queryMysql(host, "CHANGE MASTER TO MASTER_PASSWORD = 'incorrect'", struct{}{}); err != nil {
		return err
	}
	if _, err := tctx.queryMysql(host, "START SLAVE", struct{}{}); err != nil {
		return err
	}
	return nil
}

func (tctx *testContext) stepBreakReplicationOnHostInARepairableWay(host string) error {
	// Kill Replication IO thread:
	query := "SELECT id FROM information_schema.processlist WHERE state = 'Waiting for master to send event'"
	queryReqult, err := tctx.queryMysql(host, query, struct{}{})
	if err != nil {
		return err
	}
	if _, err := tctx.queryMysql(host, fmt.Sprintf("KILL %s", queryReqult[0]["id"]), struct{}{}); err != nil {
		return err
	}
	return nil
}

func (tctx *testContext) stepIGetZookeeperNode(node string) error {
	data, _, err := tctx.zk.Get(node)
	if err != nil {
		return err
	}
	tctx.zkQueryResult = string(data)
	return nil
}

func (tctx *testContext) createZookeeperNode(node string) error {
	if ok, _, err := tctx.zk.Exists(node); err == nil && ok {
		return nil
	}
	data, err := tctx.zk.Create(node, []byte{}, 0, zk.WorldACL(zk.PermAll))
	if err != nil {
		return err
	}
	tctx.zkQueryResult = data
	return nil
}

func (tctx *testContext) stepISetZookeeperNode(node string, body *gherkin.DocString) error {
	data := []byte(strings.TrimSpace(body.Content))
	if !json.Valid(data) {
		return fmt.Errorf("node value is not valid json")
	}
	_, stat, err := tctx.zk.Get(node)
	if err != nil && err != zk.ErrNoNode {
		return err
	}
	if err == zk.ErrNoNode {
		_, err = tctx.zk.Create(node, data, 0, zk.WorldACL(zk.PermAll))
	} else {
		_, err = tctx.zk.Set(node, data, stat.Version)
	}
	return err
}

func (tctx *testContext) stepIDeleteZookeeperNode(node string) error {
	_, stat, err := tctx.zk.Get(node)
	if err != nil {
		return err
	}
	return tctx.zk.Delete(node, stat.Version)
}

func (tctx *testContext) stepZookeeperNodeShouldMatch(node, matcher string, body *gherkin.DocString) error {
	err := tctx.stepIGetZookeeperNode(node)
	if err != nil {
		return err
	}
	m, err := GetMatcher(matcher)
	if err != nil {
		return err
	}
	return m(tctx.zkQueryResult, strings.TrimSpace(body.Content))
}

func (tctx *testContext) stepZookeeperNodeShouldMatchWithin(node, matcher string, timeout int, body *gherkin.DocString) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepZookeeperNodeShouldMatch(node, matcher, body)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepZookeeperNodeShouldExist(node string) error {
	err := tctx.stepIGetZookeeperNode(node)
	if err != nil {
		return err
	}
	return nil
}

func (tctx *testContext) stepZookeeperNodeShouldExistWithin(node string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepZookeeperNodeShouldExist(node)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepZookeeperNodeShouldNotExist(node string) error {
	err := tctx.stepIGetZookeeperNode(node)
	if err == zk.ErrNoNode {
		return nil
	}
	if err != nil {
		return err
	}
	return fmt.Errorf("zookeeper node %s exists, but it should not", node)
}

func (tctx *testContext) stepZookeeperNodeShouldNotExistWithin(node string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepZookeeperNodeShouldNotExist(node)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlHostShouldBeMaster(host string) error {
	res, err := tctx.queryMysql(host, "SHOW SLAVE STATUS", nil)
	if err != nil {
		return err
	}
	if len(res) != 0 {
		return fmt.Errorf("host %s has not empty slave status", host)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldHaveVariableSet(host string, name string, value string) error {
	res, err := tctx.queryMysql(host, fmt.Sprintf("SELECT @@%s AS actual", name), nil)
	if err != nil {
		return err
	}
	actual := res[0]["actual"].(string)
	if actual != value {
		return fmt.Errorf("@@%s is %s, while expected %s", name, actual, value)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldHaveVariableSetWithin(host string, name string, value string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldHaveVariableSet(host, name, value)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) shouldHaveVariableSetWithinFactory(name string, value string) func(string, int) error {
	return func(host string, timeout int) error {
		return tctx.stepMysqlHostShouldHaveVariableSetWithin(host, name, value, timeout)
	}
}

func (tctx *testContext) stepMysqlHostShouldHaveEventInStatus(host string, event string, status string) error {
	res, err := tctx.queryMysql(host, fmt.Sprintf("SELECT STATUS FROM information_schema.EVENTS WHERE CONCAT(EVENT_SCHEMA, '.', EVENT_NAME) = '%s'", event), nil)
	if err != nil {
		return err
	}
	if len(res) == 0 {
		return fmt.Errorf("event %s was not found on %s", event, host)
	}
	actual := res[0]["STATUS"].(string)
	if actual != status {
		return fmt.Errorf("event %s is %s, while expected %s", event, actual, status)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldHaveEventInStatusWithin(host string, event string, status string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldHaveEventInStatus(host, event, status)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlHostShouldBeReplicaOf(host, master string) error {
	res, err := tctx.queryMysql(host, "SHOW SLAVE STATUS", nil)
	if err != nil {
		return err
	}
	if len(res) == 0 {
		return fmt.Errorf("host %s has empty slave status", host)
	}
	masterHostStr := res[0]["Master_Host"].(string)
	if masterHostStr != master {
		return fmt.Errorf("host %s master is %s, when expected %s", host, masterHostStr, master)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldBecomeReplicaOfWithin(host, master string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldBeReplicaOf(host, master)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlReplicationOnHostShouldRunFine(host string) error {
	res, err := tctx.queryMysql(host, "SHOW SLAVE STATUS", nil)
	if err != nil {
		return err
	}
	if len(res) == 0 {
		return fmt.Errorf("host %s has empty slave status", host)
	}
	ioRunning := res[0]["Slave_IO_Running"].(string)
	ioError := res[0]["Last_IO_Error"].(string)
	if ioRunning != yes {
		return fmt.Errorf("host %s replication io thread is not running: %s", host, ioError)
	}
	sqlRunning := res[0]["Slave_SQL_Running"].(string)
	sqlError := res[0]["Last_SQL_Error"].(string)
	if sqlRunning != yes {
		return fmt.Errorf("host %s replication io thread is not running: %s", host, sqlError)
	}
	return nil
}

func (tctx *testContext) stepMysqlReplicationOnHostShouldRunFineWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlReplicationOnHostShouldRunFine(host)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlReplicationOnHostShouldNotRunFine(host string) error {
	res, err := tctx.queryMysql(host, "SHOW SLAVE STATUS", nil)
	if err != nil {
		return err
	}
	if len(res) == 0 {
		return fmt.Errorf("host %s has empty slave status", host)
	}
	ioRunning := res[0]["Slave_IO_Running"].(string)
	sqlRunning := res[0]["Slave_SQL_Running"].(string)
	if ioRunning != yes || sqlRunning != yes {
		return nil
	}
	return fmt.Errorf("host %s replication both io and sql threads are running, but should not", host)
}

func (tctx *testContext) stepMysqlReplicationOnHostShouldNotRunFineWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlReplicationOnHostShouldNotRunFine(host)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlHostShouldBecomeUnavailableWithin(host string, timeout int) error {
	addr, err := tctx.composer.GetAddr(host, mysqlPort)
	if err != nil {
		return fmt.Errorf("failed to get mysql addr %s: %s", host, err)
	}
	testutil.Retry(func() bool {
		var db *sqlx.DB
		db, err = tctx.connectMysql(addr, time.Second)
		if err == nil {
			_ = db.Close()
			return false
		}
		return true
	}, time.Duration(timeout*int(time.Second)), time.Second)
	if err == nil {
		return fmt.Errorf("mysql host %s is still available", host)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldBecomeAvailableWithin(host string, timeout int) error {
	addr, err := tctx.composer.GetAddr(host, mysqlPort)
	if err != nil {
		return fmt.Errorf("failed to get mysql addr %s: %s", host, err)
	}
	testutil.Retry(func() bool {
		var db *sqlx.DB
		db, err = tctx.connectMysql(addr, mysqlConnectTimeout)
		if err == nil {
			_ = db.Close()
			return true
		}
		return false
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) queryMysqlReadOnlyStatus(host string) (bool, bool, error) {
	res, err := tctx.queryMysql(host, "SELECT @@read_only as ro, @@super_read_only AS superRo", nil)
	if err != nil {
		return false, false, err
	}
	ro := res[0]["ro"].(string)
	superRo := res[0]["superRo"].(string)
	return ro == "1", superRo == "1", nil
}

func (tctx *testContext) stepMysqlHostShouldBeReadOnly(host string) error {
	readonly, superReadonly, err := tctx.queryMysqlReadOnlyStatus(host)
	if err != nil {
		return err
	}
	if !superReadonly || !readonly {
		return fmt.Errorf("mysql host %s is not read only: readonly=%v superReadonly=%v", host, readonly, superReadonly)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldBecomeReadOnlyWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldBeReadOnly(host)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlHostShouldBeReadOnlyNoSuper(host string) error {
	readonly, superReadonly, err := tctx.queryMysqlReadOnlyStatus(host)
	if err != nil {
		return err
	}
	if superReadonly || !readonly {
		return fmt.Errorf("mysql host %s is not read only (or super read only): readonly=%v superReadonly=%v", host, readonly, superReadonly)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldBecomeReadOnlyNoSuperWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldBeReadOnlyNoSuper(host)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepMysqlHostShouldBeWritable(host string) error {
	readonly, superReadonly, err := tctx.queryMysqlReadOnlyStatus(host)
	if err != nil {
		return err
	}
	if readonly || superReadonly {
		return fmt.Errorf("mysql host %s is not writable: readonly=%v superReadonly=%v", host, readonly, superReadonly)
	}
	return nil
}

func (tctx *testContext) stepMysqlHostShouldBecomeWritableWithin(host string, timeout int) error {
	var err error
	testutil.Retry(func() bool {
		err = tctx.stepMysqlHostShouldBeWritable(host)
		return err == nil
	}, time.Duration(timeout*int(time.Second)), time.Second)
	return err
}

func (tctx *testContext) stepISaveZookeperQueryResultAs(varname string) error {
	var j interface{}
	if tctx.zkQueryResult != "" {
		if err := json.Unmarshal([]byte(tctx.zkQueryResult), &j); err != nil {
			return err
		}
	}
	tctx.variables[varname] = j
	return nil
}

func (tctx *testContext) stepISaveSQLResultAs(varname string) error {
	tctx.variables[varname] = tctx.sqlQueryResult
	return nil
}

func (tctx *testContext) stepISaveCommandOutputAs(varname string) error {
	tctx.variables[varname] = strings.TrimSpace(tctx.commandOutput)
	return nil
}

func (tctx *testContext) stepISaveValAs(val, varname string) error {
	tctx.variables[varname] = val
	return nil
}

func (tctx *testContext) stepIWaitFor(timeout int) error {
	time.Sleep(time.Duration(timeout) * time.Second)
	return nil
}

func (tctx *testContext) stepInfoFileOnHostMatch(filepath, host, matcher string, body *gherkin.DocString) error {
	m, err := GetMatcher(matcher)
	if err != nil {
		return err
	}

	testutil.Retry(func() bool {
		remoteFile, err := tctx.composer.GetFile(host, filepath)
		if err != nil {
			return true
		}
		content, err := ioutil.ReadAll(remoteFile)
		if err != nil {
			return true
		}
		err = remoteFile.Close()
		if err != nil {
			return true
		}

		if err = m(string(content), strings.TrimSpace(body.Content)); err != nil {
			return true
		}
		return false

	}, time.Second*10, time.Second)

	return err
}

// nolint: unused
func FeatureContext(s *godog.Suite) {
	tctx, err := newTestContext()
	if err != nil {
		// TODO: how to report errors in godog
		panic(err)
	}

	s.BeforeStep(func(step *gherkin.Step) {
		tctx.templateErr = tctx.templateStep(step)
		log.Printf("STEP %d: %s %s\n", step.Node.Location.Line, step.Keyword, step.Text)
	})
	s.AfterStep(func(step *gherkin.Step, err error) {
		if tctx.templateErr != nil {
			log.Fatalf("Error in templating %s: %v\n", step.Text, tctx.templateErr)
		}
		if err != nil {
			log.Printf("ERROR %d: %s\n", step.Node.Location.Line, err)
		} else {
			log.Printf("OK\n")
		}
	})
	s.BeforeScenario(func(scenario interface{}) {
		switch s := scenario.(type) {
		case *gherkin.Scenario:
			log.Printf("SCENARIO: %s %s\n", s.Name, s.Description)
		case *gherkin.ScenarioOutline:
			log.Printf("OUTLINE: %s\n", s.Name)
		default:
			panic("unexpected type")
		}
		tctx.cleanup()
	})
	s.AfterScenario(func(scenario interface{}, err error) {
		if err != nil {
			var name string
			switch s := scenario.(type) {
			case *gherkin.Scenario:
				name = s.Name
			case *gherkin.ScenarioOutline:
				name = s.Name
			default:
				panic("unexpected type")
			}
			name = strings.Replace(name, " ", "_", -1)
			err2 := tctx.saveLogs(name)
			if err2 != nil {
				log.Printf("failed to save logs: %v", err2)
			}
		}
		tctx.cleanup()
	})

	// host manipulation
	s.Step(`^cluster environment is$`, tctx.stepClusterEnvironmentIs)
	s.Step(`^cluster is up and running$`, func() error { return tctx.stepClusterIsUpAndRunning(true) })
	s.Step(`^cluster is up and running with clean zk$`, func() error { return tctx.stepClusterIsUpAndRunning(false) })
	s.Step(`^host "([^"]*)" is stopped$`, tctx.stepHostIsStopped)
	s.Step(`^host "([^"]*)" is detached from the network$`, tctx.stepHostIsDetachedFromTheNetwork)
	s.Step(`^host "([^"]*)" is started$`, tctx.stepHostIsStarted)
	s.Step(`^host "([^"]*)" is attached to the network$`, tctx.stepHostIsAttachedToTheNetwork)
	s.Step(`^host "([^"]*)" is added`, tctx.stepHostIsAdded)
	s.Step(`^host "([^"]*)" is deleted$`, tctx.stepHostIsDeleted)

	// host checking
	s.Step(`^host "([^"]*)" should have file "([^"]*)"$`, tctx.stepHostShouldHaveFile)
	s.Step(`^host "([^"]*)" should have file "([^"]*)" within "(\d+)" seconds$`, tctx.stepHostShouldHaveFileWithin)

	// command and SQL execution
	s.Step(`^I run command on host "([^"]*)"$`, tctx.stepIRunCommandOnHost)
	s.Step(`^I run command on host "([^"]*)" with timeout "(\d+)" seconds$`, tctx.stepIRunCommandOnHostWithTimeout)
	s.Step(`^I run async command on host "([^"]*)"$`, tctx.stepIRunAsyncCommandOnHost)
	s.Step(`^I run command on host "([^"]*)" until result match regexp "([^"]*)" with timeout "(\d+)" seconds$`, tctx.stepIRunCommandOnHostUntilResultMatch)
	s.Step(`^I change replication source on host "([^"]*)" to "([^"]*)" with timeout "(\d+)" seconds$`, tctx.stepIChangeReplicationSourceWithTimeout)
	s.Step(`^command return code should be "(\d+)"$`, tctx.stepCommandReturnCodeShouldBe)
	s.Step(`^command output should match (\w+)$`, tctx.stepCommandOutputShouldMatch)
	s.Step(`^I run SQL on mysql host "([^"]*)"$`, tctx.stepIRunSQLOnHost)
	s.Step(`^SQL result should match (\w+)$`, tctx.stepSQLResultShouldMatch)
	s.Step(`^I break replication on host "([^"]*)"$`, tctx.stepBreakReplicationOnHost)
	s.Step(`^I break replication on host "([^"]*)" in repairable way$`, tctx.stepBreakReplicationOnHostInARepairableWay)
	s.Step(`^I set used space on host "([^"]*)" to (\d+)%$`, tctx.stepSetUsedSpace)

	// zookeeper manipulation
	s.Step(`^I get zookeeper node "([^"]*)"$`, tctx.stepIGetZookeeperNode)
	s.Step(`^I set zookeeper node "([^"]*)" to$`, tctx.stepISetZookeeperNode)
	s.Step(`^I delete zookeeper node "([^"]*)"$`, tctx.stepIDeleteZookeeperNode)

	// zookeeper checking
	s.Step(`^zookeeper node "([^"]*)" should match (\w+)$`, tctx.stepZookeeperNodeShouldMatch)
	s.Step(`^zookeeper node "([^"]*)" should match (\w+) within "(\d+)" seconds$`, tctx.stepZookeeperNodeShouldMatchWithin)
	s.Step(`^zookeeper node "([^"]*)" should exist$`, tctx.stepZookeeperNodeShouldExist)
	s.Step(`^zookeeper node "([^"]*)" should exist within "(\d+)" seconds$`, tctx.stepZookeeperNodeShouldExistWithin)
	s.Step(`^zookeeper node "([^"]*)" should not exist$`, tctx.stepZookeeperNodeShouldNotExist)
	s.Step(`^zookeeper node "([^"]*)" should not exist within "(\d+)" seconds$`, tctx.stepZookeeperNodeShouldNotExistWithin)

	// mysql checking
	s.Step(`^mysql host "([^"]*)" should be master$`, tctx.stepMysqlHostShouldBeMaster)
	s.Step(`^mysql host "([^"]*)" should be replica of "([^"]*)"$`, tctx.stepMysqlHostShouldBeReplicaOf)
	s.Step(`^mysql host "([^"]*)" should become replica of "([^"]*)" within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeReplicaOfWithin)
	s.Step(`^mysql replication on host "([^"]*)" should run fine$`, tctx.stepMysqlReplicationOnHostShouldRunFine)
	s.Step(`^mysql replication on host "([^"]*)" should run fine within "(\d+)" seconds$`, tctx.stepMysqlReplicationOnHostShouldRunFineWithin)
	s.Step(`^mysql replication on host "([^"]*)" should not run fine$`, tctx.stepMysqlReplicationOnHostShouldNotRunFine)
	s.Step(`^mysql replication on host "([^"]*)" should not run fine within "(\d+)" seconds$`, tctx.stepMysqlReplicationOnHostShouldNotRunFineWithin)

	s.Step(`^mysql host "([^"]*)" should become unavailable within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeUnavailableWithin)
	s.Step(`^mysql host "([^"]*)" should become available within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeAvailableWithin)
	s.Step(`^mysql host "([^"]*)" should be read only$`, tctx.stepMysqlHostShouldBeReadOnly)
	s.Step(`^mysql host "([^"]*)" should become read only within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeReadOnlyWithin)
	s.Step(`^mysql host "([^"]*)" should be read only no super$`, tctx.stepMysqlHostShouldBeReadOnlyNoSuper)
	s.Step(`^mysql host "([^"]*)" should become read only no super within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeReadOnlyNoSuperWithin)
	s.Step(`^mysql host "([^"]*)" should be writable$`, tctx.stepMysqlHostShouldBeWritable)
	s.Step(`^mysql host "([^"]*)" should become writable within "(\d+)" seconds$`, tctx.stepMysqlHostShouldBecomeWritableWithin)
	s.Step(`^mysql host "([^"]*)" should be offline within "(\d+)" seconds`, tctx.shouldHaveVariableSetWithinFactory("offline_mode", "1"))
	s.Step(`^mysql host "([^"]*)" should be online within "(\d+)" seconds$`, tctx.shouldHaveVariableSetWithinFactory("offline_mode", "0"))
	s.Step(`^mysql host "([^"]*)" should have variable "([^"]*)" set to "([^"]*)"$`, tctx.stepMysqlHostShouldHaveVariableSet)
	s.Step(`^mysql host "([^"]*)" should have variable "([^"]*)" set to "([^"]*)" within "(\d+)" seconds$`, tctx.stepMysqlHostShouldHaveVariableSetWithin)
	s.Step(`^mysql host "([^"]*)" should have event "([^"]*)" in status "([^"]*)"$`, tctx.stepMysqlHostShouldHaveEventInStatus)
	s.Step(`^mysql host "([^"]*)" should have event "([^"]*)" in status "([^"]*)" within "(\d+)" seconds$`, tctx.stepMysqlHostShouldHaveEventInStatusWithin)

	// mysql manipulation
	s.Step(`^mysql on host "([^"]*)" is killed$`, tctx.stepMysqlOnHostKilled)
	s.Step(`^mysql on host "([^"]*)" is started$`, tctx.stepMysqlOnHostStarted)
	s.Step(`^mysql on host "([^"]*)" is restarted$`, tctx.stepMysqlOnHostRestarted)
	s.Step(`^mysql on host "([^"]*)" is stopped$`, tctx.stepMysqlOnHostStopped)
	s.Step(`^I run heavy user requests on host "([^"]*)" for "(\d+)" seconds`, tctx.stepRunHeavyUserRequests)
	s.Step(`^I run long read user requests on host "([^"]*)" for "(\d+)" seconds`, tctx.stepRunHeavyReadUserRequests)
	s.Step(`^I have SQL execution error at mysql host "([^"]*)" within "(\d+)" seconds$`, tctx.stepThereIsSQLErrorWithin)
	s.Step(`^I have no SQL execution error at mysql host "([^"]*)" within "(\d+)" seconds$`, tctx.stepThereIsNoSQLErrorWithin)

	// variables
	s.Step(`^I save zookeeper query result as "([^"]*)"$`, tctx.stepISaveZookeperQueryResultAs)
	s.Step(`^I save command output as "([^"]*)"$`, tctx.stepISaveCommandOutputAs)
	s.Step(`^I save SQL result as "([^"]*)"$`, tctx.stepISaveSQLResultAs)
	s.Step(`^I save "([^"]*)" as "([^"]*)"$`, tctx.stepISaveValAs)

	// misc
	s.Step(`^I wait for "(\d+)" seconds$`, tctx.stepIWaitFor)
	s.Step(`^info file "([^"]*)" on "([^"]*)" match (\w+)$`, tctx.stepInfoFileOnHostMatch)
}

func TestMysync(t *testing.T) {
	cmd := exec.Command("cp", "../cmd/mysync/mysync", "images/mysql/mysync")
	if out, err := cmd.CombinedOutput(); err != nil {
		log.Fatalf("%s\nfailed to copy cloud/mdb/mysync/cmd/mysync/mysync: %s", out, err)
	}
	features := "features"
	if feauterEnv, ok := os.LookupEnv("GODOG_FEATURE"); ok {
		features = fmt.Sprintf("features/%s.feature", feauterEnv)
	}
	status := godog.RunWithOptions("godog", func(s *godog.Suite) {
		FeatureContext(s)
	}, godog.Options{
		Format:   "progress",
		NoColors: true,
		Paths:    []string{features},
	})
	if status > 0 {
		t.Fail()
	}
}
