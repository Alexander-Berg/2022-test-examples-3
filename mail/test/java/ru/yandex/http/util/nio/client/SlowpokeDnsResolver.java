package ru.yandex.http.util.nio.client;

import java.net.InetAddress;
import java.util.concurrent.Future;

import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;

import ru.yandex.concurrent.SingleNamedThreadFactory;
import ru.yandex.http.util.FilterFutureCallback;
import ru.yandex.http.util.nio.client.pool.SingleThreadDnsResolver;

public class SlowpokeDnsResolver extends SingleThreadDnsResolver {
    private static final long DEFAULT_DNS_UPDATE_INTERVAL = 600000L;

    private final long delay;

    public SlowpokeDnsResolver(final long delay) {
        this(SystemDefaultDnsResolver.INSTANCE, delay);
    }

    public SlowpokeDnsResolver(
        final DnsResolver dnsResolver,
        final long delay)
    {
        super(
            dnsResolver,
            DEFAULT_DNS_UPDATE_INTERVAL,
            DEFAULT_DNS_UPDATE_INTERVAL,
            new SingleNamedThreadFactory("Slowpoke-DNS"));
        this.delay = delay;
    }

    @Override
    public Future<InetAddress> resolve(
        final String hostname,
        final FutureCallback<InetAddress> callback)
    {
        return super.resolve(
            hostname,
            new FilterFutureCallback<InetAddress>(callback) {
                @Override
                public void completed(final InetAddress address) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                    }
                    this.callback.completed(address);
                }
            });
    }
}

