package ru.yandex.market.antifraud.orders.cache;

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
import org.apache.commons.lang3.NotImplementedException;

import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;

/**
 * @author dzvyagin
 */
public class MemcachedClientStub implements MemcachedClientIF {

    private final Map<String, byte[]> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Collection<SocketAddress> getAvailableServers() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Collection<SocketAddress> getUnavailableServers() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Transcoder<Object> getTranscoder() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public NodeLocator getNodeLocator() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> append(long cas, String key, Object val) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> append(String key, Object val) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> append(long cas, String key, T val, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> append(String key, T val, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> prepend(long cas, String key, Object val) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> prepend(String key, Object val) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> prepend(long cas, String key, T val, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> prepend(String key, T val, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<CASResponse> asyncCAS(String key, long casId, T value, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<CASResponse> asyncCAS(String key, long casId, Object value) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<CASResponse> asyncCAS(String key, long casId, int exp, Object value) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> OperationFuture<CASResponse> asyncCAS(String key, long casId, int exp, T value, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> CASResponse cas(String key, long casId, int exp, T value, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CASResponse cas(String key, long casId, Object value) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CASResponse cas(String key, long casId, int exp, Object value) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> CASResponse cas(String key, long casId, T value, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> add(String key, int exp, T o, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> add(String key, int exp, Object o) {
        byte[] cached = cacheMap.putIfAbsent(key, (byte[]) o);
        return new FutureValueHolder<>(cached == o);
    }

    @Override
    public <T> Future<Boolean> set(String key, int exp, T o, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> set(String key, int exp, Object o) {
        cacheMap.put(key, (byte[]) o);
        return new FutureValueHolder<>(true);
    }

    @Override
    public <T> Future<Boolean> replace(String key, int exp, T o, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> replace(String key, int exp, Object o) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<T> asyncGet(String key, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Object> asyncGet(String key) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<CASValue<Object>> asyncGetAndTouch(String key, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<CASValue<T>> asyncGetAndTouch(String key, int exp, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CASValue<Object> getAndTouch(String key, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> CASValue<T> getAndTouch(String key, int exp, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<CASValue<T>> asyncGets(String key, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<CASValue<Object>> asyncGets(String key) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> CASValue<T> gets(String key, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CASValue<Object> gets(String key) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> T get(String key, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Object get(String key) {
        return cacheMap.get(key);
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys, Iterator<Transcoder<T>> tcs) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Iterator<Transcoder<T>> tcs) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Iterator<String> keys, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Collection<String> keys, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(Iterator<String> keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(Collection<String> keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> BulkFuture<Map<String, T>> asyncGetBulk(Transcoder<T> tc, String... keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public BulkFuture<Map<String, Object>> asyncGetBulk(String... keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Map<String, T> getBulk(Iterator<String> keys, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Map<String, T> getBulk(Collection<String> keys, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<String, Object> getBulk(Iterator<String> keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<String, Object> getBulk(Collection<String> keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Map<String, T> getBulk(Transcoder<T> tc, String... keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<String, Object> getBulk(String... keys) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> touch(String key, int exp, Transcoder<T> tc) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public <T> Future<Boolean> touch(String key, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<SocketAddress, String> getVersions() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<SocketAddress, Map<String, String>> getStats() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Map<SocketAddress, Map<String, String>> getStats(String prefix) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, long by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, int by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, long by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, int by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, long by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, int by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, long by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, int by) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, long by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, int by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, long by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, int by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, long by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, int by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, long by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, int by, long def, int exp) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, long by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long incr(String key, int by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, long by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public long decr(String key, int by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, long by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncIncr(String key, int by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, long by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Long> asyncDecr(String key, int by, long def) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> delete(String key) {
        cacheMap.remove(key);
        return new FutureValueHolder<>(true);
    }

    @Override
    public Future<Boolean> delete(String key, long cas) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> flush(int delay) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Future<Boolean> flush() {
        cacheMap.clear();
        return new FutureValueHolder<>(true);
    }

    @Override
    public void shutdown() {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public boolean waitForQueues(long timeout, TimeUnit unit) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public boolean addObserver(ConnectionObserver obs) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public boolean removeObserver(ConnectionObserver obs) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CountDownLatch broadcastOp(BroadcastOpFactory of) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public CountDownLatch broadcastOp(BroadcastOpFactory of, Collection<MemcachedNode> nodes) {
        throw new NotImplementedException("It's a test stub");
    }

    @Override
    public Set<String> listSaslMechanisms() {
        throw new NotImplementedException("It's a test stub");
    }
}
