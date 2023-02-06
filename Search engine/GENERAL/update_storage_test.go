package zephyrlib

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
)

var (
	clock          clockwork.FakeClock
	storageAlpha   *UpdateStorage
	storageBravo   *UpdateStorage
	storageCharlie *UpdateStorage
)

func TestMain(m *testing.M) {
	clock = clockwork.NewFakeClockAt(time.Unix(0, 0))

	storageAlpha = NewUpdateStorageWithClock(clock)
	storageBravo = NewUpdateStorageWithClock(clock)
	storageCharlie = NewUpdateStorageWithClock(clock)

	os.Exit(m.Run())
}

func clearStorages() {
	storageAlpha.Clear()
	storageBravo.Clear()
	storageCharlie.Clear()
}

func TestConsistentMethodHashing(t *testing.T) {
	i := GetInstanceAlpha()

	m := i.Methods["/test.Service/sayHello"]
	hashAlpha, err := calcMethodHash(m)
	assert.NoError(t, err)
	hashBravo, err := calcMethodHash(m)
	assert.NoError(t, err)

	assert.Equal(t, hashAlpha, hashBravo)
}

func TestConsistentInstanceHashing(t *testing.T) {
	i := GetInstanceAlpha()

	hashAlpha, err := calcInstanceHash(i)
	assert.NoError(t, err)
	hashBravo, err := calcInstanceHash(i)
	assert.NoError(t, err)

	assert.Equal(t, hashAlpha, hashBravo)
}

func TestAPI(t *testing.T) {
	t.Run("TestAddInstances", testAddInstances)
	clearStorages()

	t.Run("TestKeepAlive", testKeepAlive)
	clearStorages()

	t.Run("TestGetUpdates", testGetUpdates)
	clearStorages()
}

func testAddInstances(t *testing.T) {
	i := GetInstanceAlpha()

	assert.NoError(t, storageAlpha.AddInstance(i))

	assert.Equal(t, 1, len(storageAlpha.instances))
	instance := storageAlpha.instances["test-production-alpha-80"]

	// Instance hash is calculated.
	assert.NotEqual(t, int64(0), instance.Hash)

	// Instance methods input/output is cleared.
	assert.Equal(t, "", instance.Methods["/test.Service/sayHello"].Input)
	assert.Equal(t, "", instance.Methods["/test.Service/sayHello"].Output)

	// Instance methods hashes are calculated.
	assert.NotEqual(t, int64(0), instance.Methods["/test.Service/sayHello"].Hash)
	assert.NotEqual(t, int64(0), instance.Methods["/test.Service/sayHelloQuickly"].Hash)
	assert.NotEqual(t, int64(0), instance.Methods["/test.Service/sayHelloOnce"].Hash)
	assert.NotEqual(t, int64(0), instance.Methods["/test.Service/ping"].Hash)

	// All methods are stored separately.
	assert.Equal(t, 2, len(storageAlpha.methods))

	helloHash := instance.Methods["/test.Service/sayHello"].Hash
	pingHash := instance.Methods["/test.Service/ping"].Hash

	// Method hashes are calculated.
	assert.NotEqual(t, int64(0), helloHash)
	assert.NotEqual(t, int64(0), pingHash)

	// Input/output is not cleared.
	assert.NotEqual(t, "", storageAlpha.methods[helloHash].Input)
	assert.NotEqual(t, "", storageAlpha.methods[helloHash].Output)
	assert.NotEqual(t, "", storageAlpha.methods[pingHash].Input)
	assert.NotEqual(t, "", storageAlpha.methods[pingHash].Output)

	// Hashes are different for methods with different input/output.
	assert.NotEqual(t, helloHash, pingHash)
}

func testKeepAlive(t *testing.T) {
	// KeepAlive on unstored instance results in error.
	assert.Error(t, storageAlpha.KeepAlive(GetInstanceAlpha()))

	// KeepAlive on stored instance updates ReportedAt.
	assert.NoError(t, storageAlpha.AddInstance(GetInstanceAlpha()))

	clock.Advance(time.Minute)
	i := GetInstanceAlpha()
	i.ReportedAt = float64(clock.Now().Unix())
	assert.NoError(t, storageAlpha.KeepAlive(i))

	assert.Equal(t, float64(clock.Now().Unix()), storageAlpha.instances["test-production-alpha-80"].ReportedAt)
}

func testGetUpdates(t *testing.T) {
	assert.NoError(t, storageAlpha.AddInstance(GetInstanceAlpha()))
	assert.NoError(t, storageBravo.AddInstance(GetInstanceAlpha()))

	// No updates from equivalent storage.
	filter := storageBravo.UpdateFilter()
	newInstances, keepaliveKeys, newMethods := storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Empty(t, newInstances)
	assert.Empty(t, keepaliveKeys)
	assert.Empty(t, newMethods)

	// Basically a copy for an empty storage.
	filter = storageCharlie.UpdateFilter()
	newInstances, keepaliveKeys, newMethods = storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Equal(t, 1, len(newInstances))
	assert.Empty(t, keepaliveKeys)
	assert.Equal(t, 2, len(newMethods))

	// No new updates after ApplyUpdates.
	assert.NoError(t, storageCharlie.ApplyUpdates(context.TODO(), newInstances, keepaliveKeys, newMethods))
	filter = storageCharlie.UpdateFilter()
	newInstances, keepaliveKeys, newMethods = storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Empty(t, newInstances)
	assert.Empty(t, keepaliveKeys)
	assert.Empty(t, newMethods)

	clock.Advance(time.Minute)
	i := GetInstanceAlpha()
	i.ReportedAt = float64(clock.Now().Unix())
	assert.NoError(t, storageAlpha.KeepAlive(i))
	clock.Advance(time.Minute)
	assert.NoError(t, storageCharlie.AddInstance(GetInstanceAlpha()))

	// Alpha has more recent instance.
	filter = storageBravo.UpdateFilter()
	newInstances, keepaliveKeys, newMethods = storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Empty(t, newInstances)
	assert.Equal(t, 1, len(keepaliveKeys))
	assert.Empty(t, newMethods)

	// Charlie has more recent instance (reverse keepalive).
	filter = storageCharlie.UpdateFilter()
	newInstances, keepaliveKeys, newMethods = storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Empty(t, newInstances)
	assert.Empty(t, keepaliveKeys)
	assert.Empty(t, newMethods)

	assert.Equal(t, float64(clock.Now().Unix()), storageAlpha.instances["test-production-alpha-80"].ReportedAt)

	// Simulate instance rebirth.
	clock.Advance(time.Minute)
	i = GetInstanceAlpha()
	i.Birth = float64(clock.Now().Unix())

	assert.NoError(t, storageAlpha.AddInstance(i))

	// Charlie should get full instance update (with no method updates).
	filter = storageCharlie.UpdateFilter()
	newInstances, keepaliveKeys, newMethods = storageAlpha.GetUpdates(filter.ExistingInstances, filter.ExistingMethodHashes)
	assert.Equal(t, 1, len(newInstances))
	assert.Empty(t, keepaliveKeys)
	assert.Empty(t, newMethods)
}
