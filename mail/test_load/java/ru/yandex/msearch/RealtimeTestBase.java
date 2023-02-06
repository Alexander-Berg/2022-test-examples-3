package ru.yandex.msearch;

import java.io.File;
import java.io.StringReader;

import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;

import ru.yandex.http.util.YandexHeaders;

import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

import ru.yandex.util.filesystem.CopyingFileVisitor;

public abstract class RealtimeTestBase extends TestBase {
    private static final Charset UTF8 = Charset.forName("utf-8");
    private static final AtomicLong queueId = new AtomicLong(0);

    public abstract Config config(final File root, final String suffix)
        throws Exception;

    public static String genQueueId() {
        return Long.toString(queueId.getAndIncrement());
    }

    public static String checkQueueId() {
        return Long.toString(queueId.get() - 1);
    }

    private static final int THREADS = 30;

    private static final int NUM_REQUESTS = 11000;
    private static final int maxmemdocs = 10000;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor( THREADS, THREADS, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(THREADS), new ThreadPoolExecutor.CallerRunsPolicy() );

    private static final AtomicInteger id = new AtomicInteger(0);

    private static final PoolingHttpClientConnectionManager httpConnManager = initHttpClient();
    private static CloseableHttpClient client;

    private static PoolingHttpClientConnectionManager initHttpClient()
    {
        PoolingHttpClientConnectionManager httpConnManager = new PoolingHttpClientConnectionManager();
        httpConnManager.setDefaultMaxPerRoute( 100 );
        httpConnManager.setMaxTotal( 100 );

        client = HttpClients.custom()
            .setConnectionManager( httpConnManager )
            .setRetryHandler( new DefaultHttpRequestRetryHandler(0, false) )
            .build();
        return httpConnManager;
    }

    private static class AddSearchTask implements Callable<Void>
    {
        private Daemon daemon;
        public AddSearchTask( Daemon daemon )
        {
            this.daemon = daemon;
        }

        @Override
        public Void call() throws Exception
        {
                int keyword = id.getAndIncrement();
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                        + "\"keyword\":\"" + keyword + "\"}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=keyword:" + keyword + "&get=keyword"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"" + keyword + "\"}]}", text);
//            }
            return null;
        }
    }

    private static class ModifySearchTask implements Callable<Void>
    {
        private Daemon daemon;
        public ModifySearchTask( Daemon daemon )
        {
            this.daemon = daemon;
        }

        @Override
        public Void call() throws Exception
        {
                int keyword = id.getAndIncrement();
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/modify");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                        + "\"keyword\":\"" + keyword + "\"}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=1&text=keyword:" + keyword + "&get=keyword"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"" + keyword + "\"}]}", text);
//            }
            return null;
        }
    }

    private static class UpdateIncrementTask implements Callable<Void>
    {
        private Daemon daemon;
        private int prefix;
        public UpdateIncrementTask( Daemon daemon, int prefix )
        {
            this.daemon = daemon;
            this.prefix = prefix;
        }

        @Override
        public Void call() throws Exception
        {
                int keyword = id.getAndIncrement();
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update?wait=0");
                post.setEntity(new StringEntity("{\"prefix\":" + prefix + ",\"docs\":[{"
                        + "\"keyword\":1,\"seq\":{\"function\":\"inc\"}}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
//            }
            return null;
        }
    }

    private static class UpdateIncrementSearchTask implements Callable<Void> {
        private final Daemon daemon;
        private final int prefix;
        private final Object lock;
        private final AtomicInteger prefixCounter;

        public UpdateIncrementSearchTask(final Daemon daemon, final int prefix,
            final Object lock, final AtomicInteger prefixCounter) {
            this.daemon = daemon;
            this.prefix = prefix;
            this.lock = lock;
            this.prefixCounter = prefixCounter;
        }

        @Override
        public Void call() throws Exception
        {
            synchronized(lock) {
                int nextSeq = prefixCounter.incrementAndGet();
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update?wait=0&prefix="
                    + prefix + "&nextSeq=" + nextSeq);
                post.setEntity(new StringEntity("{\"prefix\":" + prefix + ",\"docs\":[{"
                        + "\"keyword\":1,\"seq\":{\"inc\":[]}}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
                response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=" + prefix + "&text=keyword:1"
                    + "&get=keyword,seq,__prefix"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                try {
                    Assert.assertEquals(
                        "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"1\",\"seq\":\""
                        + nextSeq + "\",\"__prefix\":\"" + prefix + "\"}]}", text);
                } catch (Throwable t) {
                    response = client.execute(new HttpGet("http://localhost:"
                        + daemon.searchServerPort()
                        + "/search?prefix=" + prefix + "&text=keyword:1"
                        + "&get=keyword,seq,#debug_reader&collector_debug"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    System.err.println("Assert failed. Debug output: " + text);
                    throw t;
                }
            }
            return null;
        }
    }

    private static class UpdateByQueryIncrementTask implements Callable<Void>
    {
        private Daemon daemon;
        public UpdateByQueryIncrementTask( Daemon daemon )
        {
            this.daemon = daemon;
        }

        @Override
        public Void call() throws Exception
        {
                int keyword = id.getAndIncrement();
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/update?wait=0");
                post.setEntity(new StringEntity("{\"prefix\":1,\"query\":\"keyword:1\",\"docs\":[{"
                        + "\"seq\":{\"function\":\"inc\"}}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
//            }
            return null;
        }
    }

    private static class FloodModifyTask implements Callable<Void>
    {
        private Daemon daemon;
        public FloodModifyTask( Daemon daemon )
        {
            this.daemon = daemon;
        }

        @Override
        public Void call() throws Exception
        {
                HttpPost post = new HttpPost("http://localhost:" + daemon.jsonServerPort() + "/modify");
                post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                        + "\"keyword\":\"1\",\"attribute\":\"" + id.getAndIncrement() + "\"}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
//            }
            return null;
        }
    }

    @Test
    public void testAddSearch() throws Exception {
        File root = Files.createTempDirectory("testAddSearch").toFile();
        try (Daemon daemon = new Daemon(config(
                root, "primary_key = keyword\nmaxmemdocs = " + maxmemdocs)))
        {
                LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
                for( int i = 0; i < NUM_REQUESTS; i++ )
                {
                    futures.add( executor.submit( new AddSearchTask(daemon) ) );
                    Iterator<Future<Void>> iter = futures.iterator();
                    while( iter.hasNext() )
                    {
                        Future<Void> f = iter.next();
                        if( f.isCancelled() || f.isDone() )
                        {
                            f.get();
                            iter.remove();
                        }
                    }
                }
                for( Future<Void> f : futures )
                {
                    f.get();
                }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    private void sendAddRequest(
        final int indexerPort,
        final int searchPort)
        throws Exception
    {
        int keyword = id.getAndIncrement();
        HttpPost post = new HttpPost("http://localhost:"
            + indexerPort + "/add");
        post.setEntity(new StringEntity(
            "{\"prefix\":1,\"docs\":[{\"keyword\":\"" + keyword + "\"}]}",
            UTF8));
        post.addHeader(YandexHeaders.ZOO_QUEUE_ID, genQueueId());
        HttpResponse response = client.execute(post);
        Assert.assertEquals("Expected 200 OK, but received: "
            + response.getStatusLine() + " and body: "
            + EntityUtils.toString(response.getEntity()).trim(),
            HttpStatus.SC_OK,
        response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());
        response = client.execute(new HttpGet("http://localhost:"
            + searchPort
            + "/search?prefix=1&text=keyword:" + keyword + "&get=keyword"));
        String text = EntityUtils.toString(response.getEntity()).trim();
        Assert.assertEquals("Expected 200 OK, but received: "
            + response.getStatusLine() + " and body: " + text,
            HttpStatus.SC_OK,
            response.getStatusLine().getStatusCode());
        Assert.assertEquals(
            "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"" + keyword + "\"}]}", text);
    }

    private long journalsSize(final Path journalsDir) throws Exception {
        long size = 0L;
        try (DirectoryStream<Path> dir =
                Files.newDirectoryStream(journalsDir, "*.json.journal"))
        {
            for (Path journal: dir) {
                BasicFileAttributes attrs = Files.readAttributes(
                    journal,
                    BasicFileAttributes.class);
                size += attrs.size();
            }
        }
        return size;
    }

    @Test
    public void testAddSearchSingleThreadQueueId() throws Exception {
        File root = Files.createTempDirectory("testAddSearchSingleThreadQueueId").toFile();
        try (Daemon daemon = new Daemon(config(
                root, "primary_key = keyword\nmaxmemdocs = " + maxmemdocs)))
        {
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for(int i = 0; i < NUM_REQUESTS; ++i) {
                sendAddRequest(
                    daemon.jsonServerPort(),
                    daemon.searchServerPort());
            }
            Thread.sleep(3000);
            Path journalsDir = Paths.get(root + "/index/1/journal");
            long size = journalsSize(journalsDir);
            if (size == 0L) {
                sendAddRequest(
                    daemon.jsonServerPort(),
                    daemon.searchServerPort());
                Thread.sleep(3000);
                size = journalsSize(journalsDir);
            }
            YandexAssert.assertGreater(0L, size);
            File root2 =
                Files.createTempDirectory("testAddSearchSingleThreadQueueIdJournal").toFile();
            try {
                CopyingFileVisitor.copy(
                    Paths.get(root + "/index/1"),
                    Paths.get(root2 + "/index/1"));
                try (Daemon daemon2 = new Daemon(config(
                        root2,
                        "primary_key = keyword\nmaxmemdocs = " + maxmemdocs)))
                {
                    int keyword = id.get() - 1;
                    HttpResponse response = client.execute(new HttpGet("http://localhost:"
                        + daemon.searchServerPort()
                        + "/search?prefix=1&text=keyword:" + keyword + "&get=keyword"));
                    String text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(
                        "{\"hitsCount\":1,\"hitsArray\":[{\"keyword\":\"" + keyword + "\"}]}", text);
                    response = client.execute(new HttpGet(
                        "http://localhost:" + daemon2.jsonServerPort()
                        + "/getQueueId?prefix=1"));
                    text = EntityUtils.toString(response.getEntity()).trim();
                    Assert.assertEquals("Expected 200 OK, but received: "
                        + response.getStatusLine() + " and body: " + text,
                        HttpStatus.SC_OK,
                        response.getStatusLine().getStatusCode());
                    Assert.assertEquals(checkQueueId(), text);
                }
            } finally {
                SearchBackendTestBase.removeDirectory(root2);
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testModifySearch() throws Exception {
        File root = Files.createTempDirectory("testModifySearch").toFile();
        try (Daemon daemon = new Daemon(config(
                root, "primary_key = keyword\nmaxmemdocs = " + maxmemdocs)))
        {
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for( int i = 0; i < NUM_REQUESTS; i++ )
            {
                futures.add( executor.submit( new ModifySearchTask(daemon) ) );
                Iterator<Future<Void>> iter = futures.iterator();
                while( iter.hasNext() )
                {
                    Future<Void> f = iter.next();
                    if( f.isCancelled() || f.isDone() )
                    {
                        f.get();
                        iter.remove();
                    }
                }
            }
            for( Future<Void> f : futures )
            {
                f.get();
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testFloodModify() throws Exception {
        File root = Files.createTempDirectory("testFloodModifySearch").toFile();
        try (Daemon daemon = new Daemon(config(
                root, "primary_key = keyword\nmaxmemdocs = " + maxmemdocs)))
        {
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for( int i = 0; i < NUM_REQUESTS; i++ )
            {
                futures.add( executor.submit( new FloodModifyTask(daemon) ) );
                Iterator<Future<Void>> iter = futures.iterator();
                while( iter.hasNext() )
                {
                    Future<Void> f = iter.next();
                    if( f.isCancelled() || f.isDone() )
                    {
                        f.get();
                        iter.remove();
                    }
                }
            }
            for( Future<Void> f : futures )
            {
                f.get();
            }
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateIncrement() throws Exception {
        File root = Files.createTempDirectory("testUpdateIncrement").toFile();
        try (Daemon daemon = new Daemon(config(
                root,
                "primary_key = keyword\n"
                + "maxmemdocs = " + maxmemdocs + "\n"
                + "field.seq.tokenizer = keyword\n"
                + "field.seq.store = true\n"
                + "field.seq.prefixed = true\n"
                + "field.seq.analyze = true\n")))
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add?wait=0");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":1,\"seq\":\"0\"}]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for( int i = 0; i < NUM_REQUESTS; i++ )
            {
                futures.add( executor.submit( new UpdateIncrementTask(daemon, 1) ) );
                Iterator<Future<Void>> iter = futures.iterator();
                while( iter.hasNext() )
                {
                    Future<Void> f = iter.next();
                    if( f.isCancelled() || f.isDone() )
                    {
                        f.get();
                        iter.remove();
                    }
                }
            }
            for( Future<Void> f : futures )
            {
                f.get();
            }
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:1&get=seq"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"seq\":\"" + NUM_REQUESTS + "\"}]}", text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateIncrementSearchMultiPrefix() throws Exception {
        System.err.println("testUpdateIncrementSearchMultiPrefix started");
        File root = Files.createTempDirectory("testUpdateIncrementSearchMultiPrefix").toFile();
        try (Daemon daemon = new Daemon(config(
                root,
                "primary_key = keyword\n"
                + "shards = 1\n"
                + "maxmemdocs = " + maxmemdocs + "\n"
                + "field.seq.tokenizer = keyword\n"
                + "field.seq.store = true\n"
                + "field.seq.prefixed = true\n"
                + "field.seq.analyze = true\n")))
        {
            final int prefixCount = THREADS;
            //round down
            final int numRequests = NUM_REQUESTS / THREADS * THREADS;
            Object[] prefixLocks = new Object[prefixCount];
            AtomicInteger[] prefixCounters = new AtomicInteger[prefixCount];
            for (int i = 0; i < prefixCount; i++) {
                prefixLocks[i] = new Object();
                prefixCounters[i] = new AtomicInteger(0);
                HttpPost post = new HttpPost("http://localhost:"
                    + daemon.jsonServerPort() + "/add?wait=0");
                post.setEntity(new StringEntity("{\"prefix\":" + i + ",\"docs\":[{"
                    + "\"keyword\":1,\"seq\":\"0\"}]}", UTF8));
                HttpResponse response = client.execute(post);
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: "
                    + EntityUtils.toString(response.getEntity()).trim(),
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                EntityUtils.consume(response.getEntity());
            }
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for (int i = 0; i < numRequests; i++) {
                int prefix = i % prefixCount;
                futures.add(
                    executor.submit(
                        new UpdateIncrementSearchTask(
                            daemon,
                            prefix,
                            prefixLocks[prefix],
                            prefixCounters[prefix])));
                Iterator<Future<Void>> iter = futures.iterator();
                while( iter.hasNext() )
                {
                    Future<Void> f = iter.next();
                    if( f.isCancelled() || f.isDone() )
                    {
                        f.get();
                        iter.remove();
                    }
                }
            }
            for( Future<Void> f : futures )
            {
                f.get();
            }
            for (int i = 0; i < prefixCount; i++) {
                HttpResponse response = client.execute(new HttpGet("http://localhost:"
                    + daemon.searchServerPort()
                    + "/search?prefix=" + i + "&text=keyword:1&get=seq"));
                String text = EntityUtils.toString(response.getEntity()).trim();
                Assert.assertEquals("Expected 200 OK, but received: "
                    + response.getStatusLine() + " and body: " + text,
                    HttpStatus.SC_OK,
                    response.getStatusLine().getStatusCode());
                Assert.assertEquals(
                    "{\"hitsCount\":1,\"hitsArray\":[{\"seq\":\"" + (NUM_REQUESTS / prefixCount) + "\"}]}", text);
            }
        } finally {
            System.err.println("testUpdateIncrementSearchMultiPrefix ended");
            SearchBackendTestBase.removeDirectory(root);
        }
    }

    @Test
    public void testUpdateByQueryIncrement() throws Exception {
        File root = Files.createTempDirectory("testUpdateByQueryIncrement").toFile();
        try (Daemon daemon = new Daemon(config(
                root,
                "primary_key = keyword\n"
                + "maxmemdocs = " + maxmemdocs + "\n"
                + "field.seq.tokenizer = keyword\n"
                + "field.seq.store = true\n"
                + "field.seq.prefixed = true\n"
                + "field.seq.analyze = true\n")))
        {
            HttpPost post = new HttpPost("http://localhost:"
                + daemon.jsonServerPort() + "/add?wait=0");
            post.setEntity(new StringEntity("{\"prefix\":1,\"docs\":[{"
                    + "\"keyword\":1,\"seq\":\"0\"}]}", UTF8));
            HttpResponse response = client.execute(post);
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: "
                + EntityUtils.toString(response.getEntity()).trim(),
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
            LinkedList<Future<Void>> futures = new LinkedList<Future<Void>>();
            for( int i = 0; i < NUM_REQUESTS; i++ )
            {
                futures.add( executor.submit( new UpdateByQueryIncrementTask(daemon) ) );
                Iterator<Future<Void>> iter = futures.iterator();
                while( iter.hasNext() )
                {
                    Future<Void> f = iter.next();
                    if( f.isCancelled() || f.isDone() )
                    {
                        f.get();
                        iter.remove();
                    }
                }
            }
            for( Future<Void> f : futures )
            {
                f.get();
            }
            response = client.execute(new HttpGet("http://localhost:"
                + daemon.searchServerPort()
                + "/search?prefix=1&text=keyword:1&get=seq"));
            String text = EntityUtils.toString(response.getEntity()).trim();
            Assert.assertEquals("Expected 200 OK, but received: "
                + response.getStatusLine() + " and body: " + text,
                HttpStatus.SC_OK,
                response.getStatusLine().getStatusCode());
            Assert.assertEquals(
                "{\"hitsCount\":1,\"hitsArray\":[{\"seq\":\"" + NUM_REQUESTS + "\"}]}", text);
        } finally {
            SearchBackendTestBase.removeDirectory(root);
        }
    }
}

