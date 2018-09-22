package net.javacrumbs.shedlock.provider.couchbase;


import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.test.support.AbstractLockProviderIntegrationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.time.LocalDateTime;

import static com.couchbase.client.java.query.N1qlQuery.simple;
import static net.javacrumbs.shedlock.provider.couchbase.CouchbaseLockProvider.LOCKED_AT;
import static net.javacrumbs.shedlock.provider.couchbase.CouchbaseLockProvider.LOCKED_BY;
import static net.javacrumbs.shedlock.provider.couchbase.CouchbaseLockProvider.LOCK_UNTIL;
import static org.assertj.core.api.Assertions.assertThat;

public class CouchbaseLockProviderIntegrationTest extends AbstractLockProviderIntegrationTest{

    private static final String BUCKET_NAME = "bucket_1";
    private static final String HOST = "127.0.0.1";


    private CouchbaseLockProvider lockProvider;
    private Bucket bucket;
    private static Cluster cluster;

    @BeforeClass
    public static void startCouchbase () {
        cluster = connect();
    }

    @AfterClass
    public static void stopCouchbase () {
        disconnect(cluster);
    }

    @Before
    public void createLockProvider()  {
        bucket = getBucket(cluster);
        lockProvider = new CouchbaseLockProvider(bucket);
    }

    @After
    public void clear() {
        bucket.query(simple(String.format("DELETE FROM `%s`", bucket.name()), N1qlParams.build().consistency(ScanConsistency.STATEMENT_PLUS)));
    }

    @Override
    protected LockProvider getLockProvider() {
        return lockProvider;
    }

    @Override
    public void assertUnlocked(String lockName) {
        JsonDocument lockDocument = bucket.get(lockName);
        assertThat(LocalDateTime.parse((String) lockDocument.content().get(LOCK_UNTIL))).isBefore(LocalDateTime.now());
        assertThat(LocalDateTime.parse((String) lockDocument.content().get(LOCKED_AT))).isBefore(LocalDateTime.now());
        assertThat(lockDocument.content().get(LOCKED_BY)).asString().isNotEmpty();
    }

    @Override
    public void assertLocked(String lockName) {

        JsonDocument lockDocument = bucket.get(lockName);
        assertThat(LocalDateTime.parse((String) lockDocument.content().get(LOCK_UNTIL))).isAfter(LocalDateTime.now());
        assertThat(LocalDateTime.parse((String) lockDocument.content().get(LOCKED_AT))).isBefore(LocalDateTime.now());
        assertThat(lockDocument.content().get(LOCKED_BY)).asString().isNotEmpty();

    }

    private static Cluster connect(){
        return CouchbaseCluster.create(HOST);
    }

    private static void disconnect(Cluster cluster){
        cluster.disconnect();
    }

    private Bucket getBucket(Cluster cluster) {
        cluster.authenticate("Administrator", "123456");

        return cluster.openBucket(BUCKET_NAME);
    }

}