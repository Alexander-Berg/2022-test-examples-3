package app

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/cloud/mdb/mysync/internal/mysql"
	"a.yandex-team.ru/cloud/mdb/mysync/internal/mysql/gtids"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

func mustGTIDSet(s string) gtids.GTIDSet {
	gtid, err := gtids.ParseGTIDSet(gtids.MySQLFlavor, s)
	if err != nil {
		panic(err)
	}
	return gtid
}

func TestFindMostRecentNodeAndDetectSplitbrain(t *testing.T) {
	// Priority does NOT affect findMostRecentNodeAndDetectSplitbrain
	var positions []nodePosition
	var host string
	var splitbrain bool

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 5},
	}
	host, _, splitbrain = findMostRecentNodeAndDetectSplitbrain(positions)
	require.Equal(t, "A", host)
	require.False(t, splitbrain)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-102," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-103," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-104"), 0, -5},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-102," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-103"), 0, 0},
	}
	host, _, splitbrain = findMostRecentNodeAndDetectSplitbrain(positions)
	require.Equal(t, "B", host)
	require.False(t, splitbrain)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 0, 10},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 42, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5.5, 0},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 135, 0},
	}
	host, _, splitbrain = findMostRecentNodeAndDetectSplitbrain(positions)
	require.Equal(t, "C", host)
	require.False(t, splitbrain)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-101," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
	}
	host, _, splitbrain = findMostRecentNodeAndDetectSplitbrain(positions)
	require.Equal(t, "", host)
	require.True(t, splitbrain)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-000000000000:1-100"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
	}
	host, _, splitbrain = findMostRecentNodeAndDetectSplitbrain(positions)
	require.Equal(t, "", host)
	require.True(t, splitbrain)
}

func TestDesirableNodeWithZeroPriorityWorksAsFindMostRecentNode(t *testing.T) {
	// Priority does NOT affect findMostRecentNodeAndDetectSplitbrain
	var positions []nodePosition
	var host string
	logger := getLogger()

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 50*time.Second)
	require.Equal(t, "A", host)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-102," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-103," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-104"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-102," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-103"), 0, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 50*time.Second)
	require.Equal(t, "B", host)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 42, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5.5, 0},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 135, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 50*time.Second)
	require.Equal(t, "C", host)
}

func TestFindMostDesirableNodeDependsOnPriority(t *testing.T) {
	var positions []nodePosition
	var host string
	logger := getLogger()

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 5},
	}
	host, _ = getMostDesirableNode(logger, positions, 5*time.Second)
	require.Equal(t, "C", host)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100"), 0, 0},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-102," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-103," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-104"), 0, -5},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-101," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-102," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-103"), 0, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 5*time.Second)
	require.Equal(t, "C", host)

	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 0, 10},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 42, 0},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5.5, 0},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 135, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 500*time.Second)
	require.Equal(t, "A", host)

	// Priority node has huge lag, other nodes +- inside maxLag radius
	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 6000, 10},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 42, 7},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5.5, 5},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 135, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 50*time.Second)
	require.Equal(t, "B", host)

	// Priority node has huge lag, other nodes are distributed, recurse works
	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 6000, 10},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 42, 7},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5.5, 5},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 135, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 5*time.Second)
	require.Equal(t, "C", host)

	// All nodes has huge lag, nodes are close to each other, choosing most prior
	positions = []nodePosition{
		{"A", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-100," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 6000, 10},
		{"B", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 5990, 7},
		{"C", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 6005, 5},
		{"D", mustGTIDSet("6DBC0B04-4B09-43DC-86CC-9AF852DED919:1-333," +
			"09978591-5754-4710-BF67-062880ABE1B4:1-100," +
			"AA6890C8-69F8-4BC4-B3A5-5D3FEA8C28CF:1-100," +
			"708D2D2F-87E6-11EB-AEDB-E18E21FD8FD5:1-100"), 6600, 0},
	}
	host, _ = getMostDesirableNode(logger, positions, 50*time.Second)
	require.Equal(t, "A", host)
}

func TestCalcLagBytes(t *testing.T) {
	binlogs := []mysql.Binlog{
		{Name: "bin.0001", Size: 100},
		{Name: "bin.0002", Size: 200},
		{Name: "bin.0003", Size: 400},
		{Name: "bin.0004", Size: 100},
		{Name: "bin.0005", Size: 200},
		{Name: "bin.0006", Size: 400},
	}
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0003", 100)), 300+100+200+400)
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0001", 0)), 100+200+400+100+200+400)
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0000", 200)), 100+200+400+100+200+400)
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0006", 200)), 200)
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0006", 500)), 0)
	require.Equal(t, int(calcLagBytes(binlogs, "bin.0007", 100)), 0)
}

func getLogger() log.Logger {
	logLevel, _ := log.ParseLevel("fatal")
	loggerConfig := zap.KVConfig(logLevel)
	logger, _ := zap.New(loggerConfig)

	return logger
}
