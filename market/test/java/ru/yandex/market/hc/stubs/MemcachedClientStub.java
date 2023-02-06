package ru.yandex.market.hc.stubs;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.transcoders.Transcoder;

/**
 * Created by aproskriakov on 9/2/21
 */
public class MemcachedClientStub implements MemcachedClientIF {

    private final Map<String, Object> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Collection<SocketAddress> getAvailableServers() {
        return null;
    }

    @Override
    public Collection<SocketAddress> getUnavailableServers() {
        return null;
    }

    @Override
    public Transcoder<Object> getTranscoder() {
        return null;
    }

    @Override
    public NodeLocator getNodeLocator() {
        return null;
    }

    @Override
    public Future<Boolean> append(long cas, String key, Object val) {
        return null;
    }

    @Override
    public Future<Boolean> append(String key, Object val) {
        return null;
    }

    @Override
    public <T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<Boolean> append(String key, T val, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<Boolean> prepend(long cas, String key, Object val) {
        return null;
    }

    @Override
    public Future<Boolean> prepend(String key, Object val) {
        return null;
    }

    @Override
    public <T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<Boolean> prepend(String key, T val, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
        return null;
    }

    @Override
    public Future<CASResponse> asyncCAS(String key, long casId, int exp, Object value) {
        return null;
    }

    @Override
    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> CASResponse cas(String key, long casId, int exp, T value, Transcoder<T> tc) {
        return null;
    }

    @Override
    public CASResponse cas(String key, long casId, Object value) {
        return null;
    }

    @Override
    public CASResponse cas(String key, long casId, int exp, Object value) {
        return null;
    }

    @Override
    public <T> CASResponse cas(String key, long casId, T value, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<Boolean> add(String key, int exp, Object o) {
        return null;
    }

    @Override
    public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<Boolean> set(String key, int exp, Object o) {
        cacheMap.put(key, o);
        return null;
    }

    @Override
    public <T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<Boolean> replace(String key, int exp, Object o) {
        return null;
    }

    @Override
    public <T> Future<T> asyncGet(String key, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<Object> asyncGet(String key) {
        return null;
    }

    @Override
    public Future<CASValue<Object>> asyncGetAndTouch(String key, int exp) {
        return null;
    }

    @Override
    public <T> Future<CASValue<T>> asyncGetAndTouch(String key, int exp, Transcoder<T> tc) {
        return null;
    }

    @Override
    public CASValue<Object> getAndTouch(String key, int exp) {
        return null;
    }

    @Override
    public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<CASValue<T>> asyncGets(String key, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Future<CASValue<Object>> asyncGets(String key) {
        return null;
    }

    @Override
    public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
        return null;
    }

    @Override
    public CASValue<Object> gets(String key) {
        return null;
    }

    @Override
    public <T> T get(String key, Transcoder<T> tc) {
        return (T) cacheMap.get(key);
    }

    @Override
    public Object get(String key) {
        return cacheMap.get(key);
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys, Iterator<Transcoder<T>> tcs) {
        return null;
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Iterator<Transcoder<T>> tcs) {
        return null;
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Transcoder<T> tc) {
        return null;
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keys) {
        return null;
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
        return null;
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys) {
        return null;
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
        return null;
    }

    @Override
    public <T> Map<String, T> getBulk(Iterator<String> keys, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc) {
        return null;
    }

    @Override
    public Map<String, Object> getBulk(Iterator<String> keys) {
        return null;
    }

    @Override
    public Map<String, Object> getBulk(Collection<String> keys) {
        return null;
    }

    @Override
    public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
        return null;
    }

    @Override
    public Map<String, Object> getBulk(String... keys) {
        return null;
    }

    @Override
    public <T> Future<Boolean> touch(String key, int exp, Transcoder<T> tc) {
        return null;
    }

    @Override
    public <T> Future<Boolean> touch(String key, int exp) {
        return null;
    }

    @Override
    public Map<SocketAddress, String> getVersions() {
        return null;
    }

    @Override
    public Map<SocketAddress, Map<String, String>> getStats() {
        return null;
    }

    @Override
    public Map<SocketAddress, Map<String, String>> getStats(String prefix) {
        return null;
    }

    @Override
    public long incr(String key, long by) {
        return 0;
    }

    @Override
    public long incr(String key, int by) {
        return 0;
    }

    @Override
    public long decr(String key, long by) {
        return 0;
    }

    @Override
    public long decr(String key, int by) {
        return 0;
    }

    @Override
    public Future<Long> asyncIncr(String key, long by) {
        return null;
    }

    @Override
    public Future<Long> asyncIncr(String key, int by) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, long by) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, int by) {
        return null;
    }

    @Override
    public long incr(String key, long by, long def, int exp) {
        return 0;
    }

    @Override
    public long incr(String key, int by, long def, int exp) {
        return 0;
    }

    @Override
    public long decr(String key, long by, long def, int exp) {
        return 0;
    }

    @Override
    public long decr(String key, int by, long def, int exp) {
        return 0;
    }

    @Override
    public Future<Long> asyncIncr(String key, long by, long def, int exp) {
        return null;
    }

    @Override
    public Future<Long> asyncIncr(String key, int by, long def, int exp) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, long by, long def, int exp) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, int by, long def, int exp) {
        return null;
    }

    @Override
    public long incr(String key, long by, long def) {
        return 0;
    }

    @Override
    public long incr(String key, int by, long def) {
        return 0;
    }

    @Override
    public long decr(String key, long by, long def) {
        return 0;
    }

    @Override
    public long decr(String key, int by, long def) {
        return 0;
    }

    @Override
    public Future<Long> asyncIncr(String key, long by, long def) {
        return null;
    }

    @Override
    public Future<Long> asyncIncr(String key, int by, long def) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, long by, long def) {
        return null;
    }

    @Override
    public Future<Long> asyncDecr(String key, int by, long def) {
        return null;
    }

    @Override
    public Future<Boolean> delete(String key) {
        return null;
    }

    @Override
    public Future<Boolean> delete(String key, long cas) {
        return null;
    }

    @Override
    public Future<Boolean> flush(int delay) {
        return null;
    }

    @Override
    public Future<Boolean> flush() {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public boolean waitForQueues(long timeout, TimeUnit unit) {
        return false;
    }

    @Override
    public boolean addObserver(ConnectionObserver obs) {
        return false;
    }

    @Override
    public boolean removeObserver(ConnectionObserver obs) {
        return false;
    }

    @Override
    public CountDownLatch broadcastOp(BroadcastOpFactory of) {
        return null;
    }

    @Override
    public CountDownLatch broadcastOp(BroadcastOpFactory of, Collection<MemcachedNode> nodes) {
        return null;
    }

    @Override
    public Set<String> listSaslMechanisms() {
        return null;
    }
}
