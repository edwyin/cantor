package com.salesforce.cantor.s3;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.common.exceptions.SubstrateSdkException;
import com.salesforce.cantor.Namespaceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.Map;

import static com.salesforce.cantor.common.CommonPreconditions.*;

/**
 * A class responsible for managing namespace level calls for CantorOnS3
 *
 * Note: there is a cache for the namespaces that refreshes every 30 seconds, however this means that there is a chance
 * an instance of Cantor may think that a namespace exists when it doesn't.
 *
 */
public abstract class AbstractBaseS3Namespaceable implements Namespaceable {
    protected static final String NAMESPACE_IDENTIFIER = ".namespace";
    private static final Logger logger = LoggerFactory.getLogger(AbstractBaseS3Namespaceable.class);

    protected final BucketClient bucketClient;

    public AbstractBaseS3Namespaceable(final BucketClient bucketClient, final String type) throws IOException {
        checkArgument(bucketClient != null, "null bucket client");
        this.bucketClient = bucketClient;
        try {
            // validate bucketClient can connect by checking if bucket exists
            // Note: Substrate SDK doesn't have a direct head bucket operation,
            // but we can test connectivity by listing with empty prefix
            this.bucketClient.list(com.salesforce.multicloudj.blob.driver.ListBlobsRequest.builder().build()).hasNext();
        } catch (final SubstrateSdkException e) {
            logger.warn("exception validating bucket client:", e);
            throw new IOException("exception validating bucket client", e);
        }
    }

    @Override
    public void create(final String namespace) throws IOException {
        checkCreate(namespace);
        try {
            doCreate(namespace);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception creating namespace: " + namespace, e);
            throw new IOException("exception creating namespace: " + namespace, e);
        }
    }

    @Override
    public void drop(final String namespace) throws IOException {
        checkDrop(namespace);
        try {
            doDrop(namespace);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception dropping namespace: " + namespace, e);
            throw new IOException("exception dropping namespace: " + namespace, e);
        }
    }

    /**
     * Given a namespace this should return the prefix to s3 data object keys
     */
    protected abstract String getObjectKeyPrefix(final String namespace);

    private void doCreate(final String namespace) throws IOException {
        logger.info("creating namespace: '{}'.'{}'", this.bucketClient.getBucket(), namespace);
        final String markerKey = getObjectKeyPrefix(namespace) + "/" + NAMESPACE_IDENTIFIER;
        if (S3Utils.doesObjectExist(this.bucketClient, markerKey)) {
            logger.info("namespace already exists: '{}'.'{}'", namespace, this.bucketClient.getBucket());
            return;
        }
        final InputStream csvForNamespaces = new ByteArrayInputStream(("namespace=" + namespace).getBytes());
        final Map<String, String> metadata = Collections.singletonMap("Content-Type", "text/plain");
        S3Utils.putObject(this.bucketClient, markerKey, csvForNamespaces, metadata);
    }

    private void doDrop(final String namespace) throws IOException {
        logger.info("dropping namespace: '{}'.'{}'", this.bucketClient.getBucket(), namespace);

        // try to delete any data objects first in-case they are orphaned objects
        final String objectKeyPrefix = getObjectKeyPrefix(namespace);
        logger.debug("deleting all objects with prefix '{}.{}'", this.bucketClient.getBucket(), objectKeyPrefix);
        S3Utils.deleteObjects(this.bucketClient, objectKeyPrefix);
    }

    protected static String trim(final String namespace) {
        final String cleanName = namespace.replaceAll("[^A-Za-z0-9_\\-/]", "").toLowerCase();
        return String.format("%s-%s",
                cleanName.substring(0, Math.min(64, cleanName.length())),
                Math.abs(namespace.hashCode())
        );
    }
}