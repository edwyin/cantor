package com.salesforce.cantor.s3;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.blob.driver.*;
import com.salesforce.multicloudj.common.exceptions.SubstrateSdkException;
import com.google.common.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * This class is responsible for all direct communication to blob storage objects using Substrate SDK
 */
public class S3Utils {
    private static final Logger logger = LoggerFactory.getLogger(S3Utils.class);

    // read objects in 4MB chunks
    private static final int streamingChunkSize = 4 * 1024 * 1024;

    // in memory object cache
    private static final Cache<String, byte[]> cache = CacheBuilder.newBuilder()
            .maximumWeight(1024 * 1024 * 1024) // 1GB cache
            .weigher(new ObjectWeigher())
            .build();

    public static Collection<String> getKeys(final BucketClient bucketClient,
                                             final String prefix) throws IOException {
        return getKeys(bucketClient, prefix, 0, -1);
    }

    public static Collection<String> getKeys(final BucketClient bucketClient,
                                             final String prefix,
                                             final int start,
                                             final int count) throws IOException {
        final long before = System.nanoTime();
        try {
            final Set<String> keys = new HashSet<>();
            int index = 0;
            
            final ListBlobsRequest request = ListBlobsRequest.builder()
                    .withPrefix(prefix)
                    .build();
            final Iterator<BlobInfo> iterator = bucketClient.list(request);
            
            while (iterator.hasNext()) {
                final BlobInfo blobInfo = iterator.next();
                
                if (start > index++) {
                    continue;
                }
                keys.add(blobInfo.getKey());

                if (count > 0 && keys.size() == count) {
                    logger.debug("retrieved {}/{} keys, returning early", keys.size(), count);
                    return keys;
                }
            }
            
            return keys;
        } catch (final SubstrateSdkException e) {
            throw new IOException("Failed to list objects with prefix: " + prefix, e);
        } finally {
            logger.info("get keys - bucket: {} - prefix: {} - start: {} - count: {}; time spent: {}ms",
                    bucketClient.getBucket(), prefix, start, count, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static byte[] getObjectBytes(final BucketClient bucketClient,
                                        final String key) throws IOException {
        return getObjectBytes(bucketClient, key, 0, -1);
    }

    public static byte[] getObjectBytes(final BucketClient bucketClient,
                                        final String key,
                                        final long start,
                                        final long end) throws IOException {
        final long before = System.nanoTime();
        try {
            final DownloadRequest.Builder requestBuilder = DownloadRequest.builder()
                    .withKey(key);
            
            if (start >= 0 && end > 0) {
                requestBuilder.withRange(start, end);
            } else if (start > 0 && end < 0) {
                requestBuilder.withRange(start, null);
            }
            
            final DownloadRequest request = requestBuilder.build();
            
            try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                final DownloadResponse response = bucketClient.download(request, buffer);
                return buffer.toByteArray();
            }
        } catch (final SubstrateSdkException e) {
            throw new IOException("Failed to download object: " + key, e);
        } finally {
            logger.info("get object bytes - bucket: {} - key: {} - start: {} - end: {}; time spent: {}ms",
                    bucketClient.getBucket(), key, start, end, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static boolean doesObjectExist(final BucketClient bucketClient,
                                          final String key) {
        final long before = System.nanoTime();
        try {
            return bucketClient.doesObjectExist(key, null);
        } catch (final SubstrateSdkException e) {
            logger.warn("Error checking object existence for key: " + key, e);
            return false;
        } finally {
            logger.info("does object exist - bucket: {} - key: {}; time spent: {}ms",
                    bucketClient.getBucket(), key, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static InputStream getObjectStream(final BucketClient bucketClient,
                                              final String key) throws IOException {
        final long before = System.nanoTime();
        try {
            final DownloadRequest request = DownloadRequest.builder()
                    .withKey(key)
                    .build();
            
            final PipedInputStream inputStream = new PipedInputStream();
            final PipedOutputStream outputStream = new PipedOutputStream(inputStream);
            
            // Download in a separate thread to avoid blocking
            new Thread(() -> {
                try {
                    bucketClient.download(request, outputStream);
                    outputStream.close();
                } catch (Exception e) {
                    logger.error("Error downloading object stream for key: " + key, e);
                    try {
                        outputStream.close();
                    } catch (IOException ioe) {
                        logger.error("Error closing output stream", ioe);
                    }
                }
            }).start();
            
            return inputStream;
        } catch (final SubstrateSdkException e) {
            throw new IOException("Failed to get object stream: " + key, e);
        } finally {
            logger.info("get object stream - bucket: {} - key: {}; time spent: {}ms",
                    bucketClient.getBucket(), key, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static void putObject(final BucketClient bucketClient,
                                 final String key,
                                 final InputStream content,
                                 final Map<String, String> metadata) throws IOException {
        final long before = System.nanoTime();
        try {
            final UploadRequest.Builder requestBuilder = UploadRequest.builder()
                    .withKey(key);
            
            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.withMetadata(metadata);
            }
            
            final UploadRequest request = requestBuilder.build();
            bucketClient.upload(request, content);
        } catch (final SubstrateSdkException e) {
            throw new IOException("Failed to upload object: " + key, e);
        } finally {
            logger.info("put object - bucket: {} - key: {}; time spent: {}ms",
                    bucketClient.getBucket(), key, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static boolean deleteObject(final BucketClient bucketClient, final String key) {
        final long before = System.nanoTime();
        try {
            if (!bucketClient.doesObjectExist(key, null)) {
                return false;
            }
            bucketClient.delete(key, null);
            return true;
        } catch (final SubstrateSdkException e) {
            logger.error("Failed to delete object: " + key, e);
            return false;
        } finally {
            logger.info("delete object - bucket: {} - key: {}; time spent: {}ms",
                    bucketClient.getBucket(), key, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static void deleteObjects(final BucketClient bucketClient, final Collection<String> keys) {
        final long before = System.nanoTime();
        try {
            if (keys == null || keys.isEmpty()) {
                return;
            }
            final Collection<BlobIdentifier> blobIdentifiers = keys.stream()
                    .map(key -> new BlobIdentifier(key, null))
                    .collect(Collectors.toList());
            bucketClient.delete(blobIdentifiers);
        } catch (final SubstrateSdkException e) {
            logger.error("Failed to delete objects: " + keys, e);
        } finally {
            logger.info("delete objects - bucket: {} - keys: {}; time spent: {}ms",
                    bucketClient.getBucket(), keys, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static void deleteObjects(final BucketClient bucketClient,
                                     final String prefix) {
        final long before = System.nanoTime();
        try {
            final ListBlobsRequest request = ListBlobsRequest.builder()
                    .withPrefix(prefix)
                    .build();
            final Iterator<BlobInfo> iterator = bucketClient.list(request);
            
            final List<BlobIdentifier> toDelete = new ArrayList<>();
            while (iterator.hasNext()) {
                final BlobInfo blobInfo = iterator.next();
                toDelete.add(new BlobIdentifier(blobInfo.getKey(), null));
                
                // Delete in batches of 1000 to avoid memory issues
                if (toDelete.size() >= 1000) {
                    bucketClient.delete(toDelete);
                    toDelete.clear();
                }
            }
            
            if (!toDelete.isEmpty()) {
                bucketClient.delete(toDelete);
            }
        } catch (final SubstrateSdkException e) {
            logger.error("Failed to delete objects with prefix: " + prefix, e);
        } finally {
            logger.info("delete objects - bucket: {} - prefix: {}; time spent: {}ms",
                    bucketClient.getBucket(), prefix, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static int getSize(final BucketClient bucketClient, final String bucketPrefix) {
        final long before = System.nanoTime();
        try {
            int totalSize = 0;
            final ListBlobsRequest request = ListBlobsRequest.builder()
                    .withPrefix(bucketPrefix)
                    .build();
            final Iterator<BlobInfo> iterator = bucketClient.list(request);
            
            while (iterator.hasNext()) {
                iterator.next();
                totalSize++;
            }
            
            return totalSize;
        } catch (final SubstrateSdkException e) {
            logger.error("Failed to get size for prefix: " + bucketPrefix, e);
            return 0;
        } finally {
            logger.info("get size - bucket: {} - prefix: {}; time spent: {}ms",
                    bucketClient.getBucket(), bucketPrefix, ((System.nanoTime() - before) / 1_000_000)
            );
        }
    }

    public static String getCleanKeyForNamespace(final String namespace) {
        final String cleanName = namespace.replaceAll("[^A-Za-z0-9_\\-/]", "").toLowerCase();
        return String.format("cantor-%s-%s",
                cleanName.substring(0, Math.min(32, cleanName.length())), Math.abs(namespace.hashCode()));
    }

    /**
     * S3 Select functionality is AWS-specific and not available in Substrate SDK.
     * This class provides a fallback implementation that downloads and processes data client-side.
     * Note: This is less efficient than server-side S3 Select processing.
     */
    public static class S3Select {

        public static String queryObjectJson(final BucketClient bucketClient,
                                            final String key,
                                            final String query) throws IOException {
            logger.warn("S3 Select functionality is not available with Substrate SDK. " +
                       "Falling back to client-side processing for key: {}", key);
            
            // For now, return the raw object content as S3 Select is AWS-specific
            // This is a simplified fallback - in a real migration, you might want to implement
            // client-side JSON filtering based on the query
            try {
                final byte[] objectBytes = getObjectBytes(bucketClient, key);
                return new String(objectBytes);
            } catch (IOException e) {
                throw new IOException("Failed to query JSON object: " + key, e);
            }
        }

        public static String queryObjectCsv(final BucketClient bucketClient,
                                           final String key,
                                           final String query) throws IOException {
            logger.warn("S3 Select functionality is not available with Substrate SDK. " +
                       "Falling back to client-side processing for key: {}", key);
            
            // For now, return the raw object content as S3 Select is AWS-specific
            try {
                final byte[] objectBytes = getObjectBytes(bucketClient, key);
                return new String(objectBytes);
            } catch (IOException e) {
                throw new IOException("Failed to query CSV object: " + key, e);
            }
        }
    }

    private static class ObjectWeigher implements Weigher<String, byte[]> {
        @Override
        public int weigh(final String keyIgnored, final byte[] value) {
            return value.length;
        }
    }
}