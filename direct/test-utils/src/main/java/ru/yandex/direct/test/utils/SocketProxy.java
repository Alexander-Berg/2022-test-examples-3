package ru.yandex.direct.test.utils;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.direct.utils.Checked;

/**
 * Предназначен для тестирования сокетов, где затруднительно подлезть моками.
 * <p>
 * Запускает сервер, который пересылает все входящие данные в другой хост-порт.
 * <p>
 * Предоставляет интерфейс для man-in-the-middle, который может изменить передаваемые данные или закрыть соединение.
 * <p>
 * Имеет топорную и неэффективную реализацию, которая сильно грузит GC, поэтому подходит только для тестов.
 */
@ParametersAreNonnullByDefault
public class SocketProxy extends Thread implements Closeable {
    /**
     * Маркер, который означает, что сокет между прокси и настоящим сервером следует закрыть.
     */
    private static final ByteBuffer CLOSE = ByteBuffer.allocate(1);
    /**
     * Буфер на чтение, размера которого хватит всем. Так как все сокеты обрабатываются в одном потоке, одного буфера
     * достаточно.
     */
    private static final ByteBuffer SHARED_READ_BUFFER = ByteBuffer.allocate(640_000);
    private final Map<SocketChannel, Queue<ByteBuffer>> writeBuffers = new HashMap<>();
    private final Map<SocketChannel, SocketChannel> proxies = new IdentityHashMap<>();
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final InetSocketAddress listenAddress;
    private int concurrency = 1;
    private int connectionsNow = 0;
    private InetSocketAddress destinationAddress;
    private ServerSocketChannel serverSocket;
    private volatile boolean debug = false;
    private List<ManInTheMiddle> manInTheMiddleList = new ArrayList<>();

    /**
     * @param listenAddress      Адрес для прослушивания.
     * @param destinationAddress Адрес, куда будут перенаправляться данные.
     */
    public SocketProxy(@Nullable InetSocketAddress listenAddress, InetSocketAddress destinationAddress) {
        this.listenAddress = listenAddress;
        this.destinationAddress = destinationAddress;
    }

    /**
     * Автоматически выбирает произвольный свободный порт на машине для прослушивания.
     *
     * @param destinationAddress Адрес, куда будут перенаправляться данные.
     */
    public SocketProxy(InetSocketAddress destinationAddress) {
        this(null, destinationAddress);
    }

    /**
     * @return Выводить ли подробную информацию о происходящих событиях в stderr.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug Выводить ли подробную информацию о происходящих событиях в stderr.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return Адрес, который слушает прокси-сервер.
     */
    public SocketAddress getListenAddress() throws InterruptedException {
        readyLatch.await();
        return Checked.get(serverSocket::getLocalAddress);
    }

    /**
     * @return Флаг, по которому можно определить, что прокси готов принимать подключения.
     */
    public CountDownLatch getReadyLatch() {
        return readyLatch;
    }

    /**
     * @return Максимальное поддерживаемое количество подключений.
     */
    public int getConcurrency() {
        return concurrency;
    }

    /**
     * @param concurrency Максимальное поддерживаемое количество подключений.
     */
    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    /**
     * Добавить объект, который будет прослушивать трафик и по желанию изменять его или обрывать соединение.
     */
    public void addDataInterceptor(ManInTheMiddle manInTheMiddle) {
        manInTheMiddleList.add(manInTheMiddle);
    }

    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(listenAddress);
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT, null);

            readyLatch.countDown();

            while (!isInterrupted()) {
                selector.select(100);
                boolean didSomething = false;
                for (SelectionKey key : selector.selectedKeys()) {
                    try {
                        if (key.isAcceptable() && connectionsNow < concurrency) {
                            didSomething = onAccept(selector);
                        }
                        if (key.isReadable()) {
                            didSomething |= onRead(key);
                        }
                        if (key.isWritable()) {
                            didSomething |= onWrite(key);
                        }
                    } catch (CancelledKeyException exc) {
                        // ignore
                    } catch (ClosedChannelException ignored) {
                        if (onCancelledKey(key)) {
                            return;
                        }
                    }
                }
                if (!didSomething) {
                    Thread.sleep(10);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            interrupt();
        }
    }

    private boolean onCancelledKey(SelectionKey key) throws IOException {
        if (key.channel() instanceof ServerSocketChannel) {
            return true;
        } else if (key.channel() instanceof SocketChannel) {
            key.channel().close();
            SocketChannel other = proxies.remove((SocketChannel) key.channel());
            if (debug) {
                System.err.printf("Closed channel: %s%n",
                        ((SocketChannel) key.channel()).getRemoteAddress());
            }
            if (other != null) {
                if (debug) {
                    System.err.printf("Closing it's opposite: %s%n", other.getRemoteAddress());
                }
                writeBuffers.get(other).add(CLOSE);
                proxies.remove(other);
            }
            key.cancel();
        } else {
            throw new IllegalStateException(key.channel().toString());
        }
        return false;
    }

    private boolean onAccept(Selector selector) throws IOException {
        SocketChannel source = serverSocket.accept();
        if (source != null) {
            boolean close = false;
            for (ManInTheMiddle manInTheMiddle : manInTheMiddleList) {
                if (!manInTheMiddle.connectionRequested(source)) {
                    if (debug) {
                        System.err.printf("Interceptor %s closes socket%n", manInTheMiddle);
                    }
                    close = true;
                    break;
                }
            }
            if (close) {
                System.err.printf("Interceptor closed connection from %s%n",
                        source.getRemoteAddress());
            } else {
                writeBuffers.computeIfAbsent(source, k -> new ArrayDeque<>());
                source.configureBlocking(false);
                source.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                SocketChannel sink = SocketChannel.open(destinationAddress);
                writeBuffers.computeIfAbsent(sink, k -> new ArrayDeque<>());
                sink.configureBlocking(false);
                sink.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                proxies.put(source, sink);
                proxies.put(sink, source);
                ++connectionsNow;
                if (debug) {
                    System.err.printf("Proxy %s -> %s%n",
                            serverSocket.getLocalAddress(),
                            destinationAddress);
                }
            }

            return true;
        }
        return false;
    }

    private boolean onRead(SelectionKey key) throws IOException {
        SocketChannel readable = (SocketChannel) key.channel();
        SocketChannel writable = proxies.get(readable);
        // Для простого отправителя позволяется держать в очереди не более одного пакета.
        // По факту максимум может быть два буфера: один с данными и один CLOSE.
        Queue<ByteBuffer> writeBuffer = writeBuffers.get(writable);
        if (!writeBuffer.isEmpty()) {
            return false;
        }
        ByteBuffer buffer = null;
        boolean close = false;
        boolean result = false;
        try {
            SHARED_READ_BUFFER.clear();
            readable.read(SHARED_READ_BUFFER);
            if (SHARED_READ_BUFFER.position() == 0) {
                return false;
            }
            byte[] data = new byte[SHARED_READ_BUFFER.position()];
            SHARED_READ_BUFFER.rewind();
            SHARED_READ_BUFFER.get(data);
            if (debug) {
                System.out.printf("Read from %s:%s: ```%s```%n",
                        readable.getLocalAddress(),
                        readable.getRemoteAddress(),
                        new String(data, Charsets.UTF_8));
            }
            for (ManInTheMiddle manInTheMiddle : manInTheMiddleList) {
                Pair<byte[], Boolean> newDataAndKeep =
                        manInTheMiddle.transferRequested(readable, writable, data);
                data = Objects.requireNonNull(newDataAndKeep.getKey());
                if (!newDataAndKeep.getValue()) {
                    if (debug) {
                        System.err.printf("Interceptor %s closes socket%n", manInTheMiddle);
                    }
                    readable.close();
                    close = true;
                    break;
                }
            }
            buffer = ByteBuffer.wrap(data);
        } catch (IOException exc) {
            if (debug) {
                System.out.printf("Error on %s: %s%n", readable.getRemoteAddress(), exc.getMessage());
            }
            close = true;
        }
        if (buffer != null && buffer.position() < buffer.limit()) {
            writeBuffer.add(buffer);
            result = true;
        }
        if (close) {
            writeBuffer.add(CLOSE);
            readable.close();
            key.cancel();
            result = true;
        }
        return result;
    }

    private boolean onWrite(SelectionKey key) throws IOException {
        SocketChannel writable = (SocketChannel) key.channel();
        ByteBuffer buffer = writeBuffers.get(writable).peek();
        if (buffer != null) {
            if (buffer == CLOSE) {
                if (debug) {
                    System.out.printf("Close %s:%s%n",
                            writable.getLocalAddress(),
                            writable.getRemoteAddress());
                }
                writable.close();
                writeBuffers.remove(writable);
                key.cancel();
                --connectionsNow;
            } else {
                if (debug) {
                    System.out.printf("Write to %s:%s: ```%s```%n",
                            writable.getLocalAddress(),
                            writable.getRemoteAddress(),
                            new String(buffer.array(), Charsets.UTF_8));
                }
                writable.write(buffer);
                if (buffer.remaining() == 0) {
                    writeBuffers.get(writable).remove();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        interrupt();
    }

    /**
     * Интерфейс для прослушивания соединений в прокси-сервере.
     */
    public interface ManInTheMiddle {
        /**
         * Вызывается при запросе на установление соединения.
         *
         * @param source Источник, который подключается к серверу.
         * @return true - если соединение следует одобрить, false - если следует разорвать.
         */
        boolean connectionRequested(SocketChannel source);

        /**
         * Вызывается при передаче пачки данных, как от клиента к серверу, так и от сервера к клиенту.
         *
         * @param source Отправляющая сторона.
         * @param sink   Принимающая сторона.
         * @param data   Данные, которые отправитель хочет передать получателю.
         * @return Пара.<ol>
         * <li>Данные, которые действительно будут переданы получателю.</li>
         * <li>true - оставить соединение. false - соединение следует порвать сразу после передачи данных. Если
         * хочется порвать немедленно, то стоит передать пустые данные.</li>
         * </ol>
         */
        Pair<byte[], Boolean> transferRequested(SocketChannel source, SocketChannel sink, byte[] data);
    }
}
