package tariffmatcher

import (
	"regexp"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestRegexpCache(t *testing.T) {
	t.Run(
		"RegexpCache", func(t *testing.T) {
			cache := NewRegexpCache()

			regexpText := "a.*"
			compiledRegexp, err := regexp.Compile(regexpText)
			require.NoError(t, err)
			require.EqualValues(t, true, cache.MatchString(compiledRegexp, "abc"))
			require.EqualValues(t, false, cache.MatchString(compiledRegexp, "xyz"))

			knownKeys := []CacheValue{
				CacheValue{regexpText, "abc"},
				CacheValue{regexpText, "xyz"},
			}
			// yield the control to another thread, so all the cache values would be up to date
			time.Sleep(10 * time.Millisecond)
			cachedKeys := cache.(*RegexpCacheImpl).getCachedKeys()
			sort.SliceStable(cachedKeys, func(i, j int) bool {
				return cachedKeys[i].regexpString+cachedKeys[i].tariffCode < cachedKeys[j].regexpString+cachedKeys[j].tariffCode
			})
			require.EqualValues(t, knownKeys, cachedKeys)

			// see if getCacheValue works
			result, ok := cache.(*RegexpCacheImpl).getCacheValue(CacheValue{regexpText, "abc"})
			require.True(t, ok)
			require.True(t, result)

			result, ok = cache.(*RegexpCacheImpl).getCacheValue(CacheValue{regexpText, "xyz"})
			require.True(t, ok)
			require.False(t, result)

			result, ok = cache.(*RegexpCacheImpl).getCacheValue(CacheValue{regexpText, "123"})
			require.False(t, ok)
			require.False(t, result)

			regexpText2 := "ab.*"
			compiledRegexp2, err := regexp.Compile(regexpText2)
			require.NoError(t, err)
			// Make sure that the same "abc" string is allowed be cached as a match for more than a single regexp
			require.EqualValues(t, true, cache.MatchString(compiledRegexp2, "abc"))
		},
	)
}
