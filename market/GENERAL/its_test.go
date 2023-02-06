package its

import (
	"fmt"
	"strings"
	"sync"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

type entry struct {
	key string
	val interface{}
}

func quote(s string) string {
	return "\"" + s + "\""
}

func makeJSON(entries ...entry) string {
	slist := make([]string, 0, len(entries))
	for _, e := range entries {
		switch e.val.(type) {
		case bool:
			val := e.val.(bool)
			slist = append(slist, fmt.Sprintf("%s: %t", quote(e.key), val))
		case string:
			val := e.val.(string)
			slist = append(slist, fmt.Sprintf("%s: %s", quote(e.key), quote(val)))
		case int:
			val := int(e.val.(int))
			slist = append(slist, fmt.Sprintf("%s: %d", quote(e.key), val))
		}
	}
	return "{" + strings.Join(slist, ",") + "}"
}

func TestRead(t *testing.T) {
	const JSON = `{"ii": 42, "ss": "lala", "ff": 3.14, "empty": "", "b1": true, "b0": false, "list": [11, 22.5]}`
	settings, err := NewSettingsFromString(JSON, SettingsOptions{})
	fmt.Printf("%#v\n", settings)
	require.NoError(t, err)

	boolSpecs := []struct {
		key  string
		ok   bool
		want bool
	}{
		{
			key:  "b1",
			ok:   true,
			want: true,
		},
		{
			key:  "b0",
			ok:   true,
			want: false,
		},
		{
			key: "empty",
			ok:  false,
		},
		{
			key: "absent",
			ok:  false,
		},
		{
			key: "ii",
			ok:  false,
		},
	}
	for _, spec := range boolSpecs {
		val, ok := settings.FindBool(spec.key)
		require.Equal(t, spec.ok, ok)
		if spec.ok {
			require.Equal(t, spec.want, val)
		}
	}
	{
		ii, ok := settings.FindInt("ii")
		require.True(t, ok)
		require.Equal(t, 42, ii)
	}
	{
		ss, ok := settings.FindString("ss")
		require.True(t, ok)
		require.Equal(t, "lala", ss)
	}
	{
		ff, ok := settings.FindFloat("ff")
		require.True(t, ok)
		require.Equal(t, 3.14, ff)
	}
	{
		_, ok := settings.FindInt("")
		require.False(t, ok)
	}
	{
		_, ok := settings.FindString("")
		require.False(t, ok)
	}
	{
		{
			_, ok := settings.FindInt("empty")
			require.False(t, ok)
		}
		{
			_, ok := settings.FindFloat("empty")
			require.False(t, ok)
		}
	}
	require.Equal(t, []int{1}, settings.GetIntList("empty", []int{1}, Any))
	require.Equal(t, []int{11, 22}, settings.GetIntList("list", []int{1}, Any))
}

func TestRace(t *testing.T) {
	reader := StringSettingsReader{`{"foo": 42}`}
	holder, err := NewSettingsHolder(&reader, SettingsOptions{})
	require.NoError(t, err)

	check := func() {
		foo, ok := holder.GetSettings().FindInt("foo")
		require.True(t, ok)
		require.Equal(t, 42, foo)
	}

	check()

	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		err := holder.Update()
		assert.NoError(t, err)
		wg.Done()
	}()
	check()

	wg.Wait()
	check()
}

func TestGetForcedGeneration(t *testing.T) {
	const proxy = "hahn"
	const generationName = "20201112_112233"
	{
		json := makeJSON(
			entry{"generation_proxy", proxy},
			entry{"generation_name", generationName},
		)
		settings, err := NewSettingsFromString(json, SettingsOptions{})
		require.NoError(t, err)

		forcedGen, err := GetForcedGeneration(settings)
		require.NoError(t, err)
		require.Equal(t, forcedGen.Proxy, proxy)
		require.Equal(t, forcedGen.Name, generationName)
	}
	{
		json := makeJSON(
			entry{"generation_proxy", proxy},
		)
		settings, err := NewSettingsFromString(json, SettingsOptions{})
		require.NoError(t, err)

		forcedGen, err := GetForcedGeneration(settings)
		require.NoError(t, err)
		require.Equal(t, forcedGen.Proxy, proxy)
		require.Equal(t, forcedGen.Name, "recent")
	}
}

func TestNoSettings(t *testing.T) {
	var holder *SettingsHolder
	settings := holder.GetSettings()
	require.Equal(t, 42, settings.GetInt("lala", 42, Any))
}

func TestDataCenterSettings(t *testing.T) {
	json := makeJSON(
		entry{"foo:foo", 1},
		entry{"bar:bar", true},
		entry{"bar1:foo", 2},
		entry{"baz", true},
	)
	{
		settings, err := NewSettingsFromString(json, SettingsOptions{DCPrefix: "foo"})
		require.NoError(t, err)
		require.Equal(t, 1, settings.GetInt("foo", 0, Any))
		require.Equal(t, false, settings.GetBool("bar", false, Any))
		require.Equal(t, true, settings.GetBool("baz", false, Any))
	}
	{
		settings, err := NewSettingsFromString(json, SettingsOptions{DCPrefix: "bar"})
		require.NoError(t, err)
		require.Equal(t, 0, settings.GetInt("foo", 0, Any))
		require.Equal(t, true, settings.GetBool("bar", false, Any))
		require.Equal(t, true, settings.GetBool("baz", false, Any))
	}
	{
		settings, err := NewSettingsFromString(json, SettingsOptions{DCPrefix: "bar1"})
		require.NoError(t, err)
		require.Equal(t, 2, settings.GetInt("foo", 0, Any))
		require.Equal(t, false, settings.GetBool("bar", false, Any))
		require.Equal(t, true, settings.GetBool("baz", false, Any))
	}
	{
		settings, err := NewSettingsFromString(json, SettingsOptions{})
		require.NoError(t, err)
		require.Equal(t, 0, settings.GetInt("foo", 0, Any))
		require.Equal(t, false, settings.GetBool("bar", false, Any))
		require.Equal(t, true, settings.GetBool("baz", false, Any))
	}
}

func TestPrefix(t *testing.T) {
	options := SettingsOptions{CustomPrefix: "sas"}
	{
		const JSON = `{"foo": 1, "sas:foo": 42}`
		settings, err := NewSettingsFromString(JSON, options)
		require.NoError(t, err)
		require.Equal(t, 42, settings.GetInt("foo", 0, Any))
		//
		holder, err := NewStringSettingsHolder2(JSON, options)
		require.NoError(t, err)
		settings2 := holder.GetSettings()
		require.Equal(t, 42, settings2.GetInt("foo", 0, Any))
	}
	{
		const JSON = `{"foo": false, "sas:foo": true}`
		settings, err := NewSettingsFromString(JSON, options)
		require.NoError(t, err)
		require.Equal(t, true, settings.GetBool("foo", false, Any))
	}
	{
		const JSON = `{"sas:foo": true, "foo": false}`
		settings, err := NewSettingsFromString(JSON, options)
		require.NoError(t, err)
		require.Equal(t, true, settings.GetBool("foo", false, Any))
	}
	{
		const JSON = `{"foo": "lala", "sas:foo": "ok"}`
		settings, err := NewSettingsFromString(JSON, options)
		require.NoError(t, err)
		require.Equal(t, "ok", settings.GetString("foo", "", Any))
	}
}

func TestConstraints(t *testing.T) {
	opts := SettingsOptions{
		DCPrefix:       "sas",
		InstancePrefix: "sas1",
	}
	{ // проверяем ограничители
		test := func(s string, dc, instance, global, any bool) {
			stx, err := NewSettingsFromString(s, opts)
			require.NoError(t, err)
			require.Equal(t, dc, stx.GetBool("flag", false, DC))
			require.Equal(t, instance, stx.GetBool("flag", false, Instance))
			require.Equal(t, global, stx.GetBool("flag", false, Global))
			require.Equal(t, any, stx.GetBool("flag", false, Any))
		}
		test(`{"flag": true}`, false, false, true, true)
		test(`{"sas:flag": true}`, true, false, false, true)
		test(`{"man:flag": true}`, false, false, false, false)
		test(`{"sas1:flag": true}`, false, true, false, true)
		test(`{"sas2:flag": true}`, false, false, false, false)
		test(`{"man1:flag": true}`, false, false, false, false)
	}
	{ // проверяем приоритетность
		stx, err := NewSettingsFromString(`{"flag": 1, "sas:flag":2, "sas1:flag":3}`, opts)
		require.NoError(t, err)
		require.Equal(t, 3, stx.GetInt("flag", 0, Any))
		require.Equal(t, 0, stx.GetInt("flag", 0, Global))
		require.Equal(t, 0, stx.GetInt("flag", 0, DC))
		require.Equal(t, 3, stx.GetInt("flag", 0, Instance))
	}
}
