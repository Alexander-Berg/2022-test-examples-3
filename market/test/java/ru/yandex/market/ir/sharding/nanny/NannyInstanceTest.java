package ru.yandex.market.ir.matcher2.tools.nanny;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

public class NannyInstanceTest {

    @Test
    public void getDc() {
        NannyInstance instance = new NannyInstance("localhost", 8080, Set.of("itag_replica_0", "a_dc_sas", "a_ctype_testing"));
        assertEquals("sas", instance.getDc());
    }

    @Test(expected = IllegalStateException.class)
    public void getDc_notFound() {
        new NannyInstance("localhost", 8080, Set.of("itag_replica_0", "a_ctype_testing"));
    }

    @Test
    public void getEnvironment() {
        NannyInstance instance = new NannyInstance("localhost", 8080, Set.of("itag_replica_0", "a_dc_sas", "a_ctype_testing"));
        assertEquals("testing", instance.getEnv());
    }

    @Test(expected = IllegalStateException.class)
    public void getEnvironment_notFound() {
        new NannyInstance("localhost", 8080, Set.of("itag_replica_0", "a_dc_sas"));
    }

    @Test
    public void getReplicaId() {
        NannyInstance instance = new NannyInstance("localhost", 8080, Set.of("itag_replica_3", "a_dc_sas", "a_ctype_testing"));
        assertEquals(3, instance.getReplicaId());
    }

    @Test(expected = IllegalStateException.class)
    public void getReplicaId_notFound() {
        new NannyInstance("localhost", 8080, Set.of("a_dc_sas", "a_ctype_testing"));
    }

    @Test
    public void getShardId() {
        NannyInstance instance = new NannyInstance("localhost", 8080, Set.of("itag_replica_3", "a_dc_sas", "a_ctype_testing", "a_shard_5"));
        assertEquals(5, instance.getShardId());
    }

    @Test
    public void getShardId_notFound() {
        NannyInstance instance = new NannyInstance("localhost", 8080, Set.of("itag_replica_3", "a_dc_sas", "a_ctype_testing"));
        assertEquals(0, instance.getShardId());
    }
}
