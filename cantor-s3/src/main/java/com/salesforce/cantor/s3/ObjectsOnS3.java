/*
 * Copyright (c) 2020, Salesforce.com, Inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.cantor.s3;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.common.exceptions.SubstrateSdkException;
import com.salesforce.cantor.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salesforce.cantor.common.CommonPreconditions.checkArgument;
import static com.salesforce.cantor.common.CommonPreconditions.checkString;
import static com.salesforce.cantor.common.ObjectsPreconditions.*;

public class ObjectsOnS3 extends AbstractBaseS3Namespaceable implements Objects {
    private static final Logger logger = LoggerFactory.getLogger(ObjectsOnS3.class);

    // cantor-objects-<namespace>/<startTimestamp>-<endTimestamp>
    private static final String objectKeyPrefix = "cantor-objects";

    public ObjectsOnS3(final BucketClient bucketClient) throws IOException {
        super(bucketClient, "objects");
    }

    @Override
    public void store(final String namespace, final String key, final byte[] bytes) throws IOException {
        checkStore(namespace, key, bytes);
        try {
            doStore(namespace, key, new ByteArrayInputStream(bytes), bytes.length);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception storing object: " + namespace + "." + key, e);
            throw new IOException("exception storing object: " + namespace + "." + key, e);
        }
    }

    @Override
    public byte[] get(final String namespace, final String key) throws IOException {
        checkGet(namespace, key);
        try {
            return doGet(namespace, key);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting object: "  + namespace + "." + key, e);
            throw new IOException("exception getting object: " + namespace + "." + key, e);
        }
    }

    @Override
    public boolean delete(final String namespace, final String key) throws IOException {
        checkDelete(namespace, key);
        try {
            return S3Utils.deleteObject(this.bucketClient, getObjectKey(namespace, key));
        } catch (final SubstrateSdkException e) {
            logger.warn("exception deleting object: " + namespace + "." + key, e);
            throw new IOException("exception deleting object: " + namespace + "." + key, e);
        }
    }

    @Override
    public Collection<String> keys(final String namespace, final int start, final int count) throws IOException {
        checkKeys(namespace, start, count);
        try {
            return doKeys(namespace, "", start, count);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting keys of namespace: " + namespace, e);
            throw new IOException("exception getting keys of namespace: " + namespace, e);
        }
    }

    @Override
    public Collection<String> keys(final String namespace, final String prefix, final int start, final int count) throws IOException {
        checkKeys(namespace, start, count);
        try {
            return doKeys(namespace, prefix, start, count);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting keys of namespace: " + namespace, e);
            throw new IOException("exception getting keys of namespace: " + namespace, e);
        }
    }

    @Override
    public int size(final String namespace) throws IOException {
        checkSize(namespace);
        try {
            return doSize(namespace);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting size of namespace: " + namespace, e);
            throw new IOException("exception getting size of namespace: " + namespace, e);
        }
    }

    public void store(final String namespace, final String key, final InputStream stream, final long length) throws IOException {
        checkString(namespace);
        checkString(key);
        checkArgument(stream != null, "null stream");
        checkArgument(length > 0, "zero/negative length");
        try {
            doStore(namespace, key, stream, length);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception storing stream:", e);
        }
    }

    public InputStream stream(final String namespace, final String key) throws IOException {
        checkString(namespace);
        checkString(key);
        try {
            return doStream(namespace, key);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception streaming:", e);
            return null;
        }
    }

    private void doStore(final String namespace, final String key, final InputStream stream, final long length) throws IOException {
        checkNamespace(namespace);
        final String objectName = getObjectKey(namespace, key);

        final Map<String, String> metadata = Collections.singletonMap("Content-Length", String.valueOf(length));
        logger.info("storing stream with length={} at '{}.{}'", length, this.bucketClient.getBucket(), objectName);
        // if no exception is thrown, the object was put successfully - ignore response value
        S3Utils.putObject(this.bucketClient, objectName, stream, metadata);
    }

    private byte[] doGet(final String namespace, final String key) throws IOException {
        final String objectName = getObjectKey(namespace, key);
        logger.debug("retrieving object at '{}.{}'", this.bucketClient.getBucket(), objectName);
        if (!S3Utils.doesObjectExist(this.bucketClient, objectName)) {
            return null;
        }
        return S3Utils.getObjectBytes(this.bucketClient, objectName);
    }

    private InputStream doStream(final String namespace, final String key) throws IOException {
        final String objectName = getObjectKey(namespace, key);
        if (!this.bucketClient.doesObjectExist(objectName, null)) {
            throw new IOException(String.format("couldn't find objectName '%s' for namespace '%s'", objectName, namespace));
        }
        return S3Utils.getObjectStream(this.bucketClient, objectName);
    }

    private int doSize(final String namespace) {
        return S3Utils.getSize(this.bucketClient, getObjectKey(namespace, ""));
    }

    private Collection<String> doKeys(final String namespace, final String prefix, final int start, final int count) throws IOException {
        final String namespaceObjectPrefix = getObjectKey(namespace, prefix);
        return S3Utils.getKeys(this.bucketClient, namespaceObjectPrefix, start, count)
                .stream()
                .filter(key -> !key.endsWith(NAMESPACE_IDENTIFIER))
                .map(objectFile -> objectFile.substring(namespaceObjectPrefix.length()))
                .collect(Collectors.toList());
    }

    private String getObjectKey(final String namespace, final String key) {
        return String.format("%s/%s", getObjectKeyPrefix(namespace), key);
    }

    @Override
    protected String getObjectKeyPrefix(final String namespace) {
        return String.format("%s/%s", objectKeyPrefix, trim(namespace));
    }

}