package com.salesforce.cantor.s3;

import com.salesforce.multicloudj.blob.client.BucketClient;
import com.salesforce.multicloudj.common.exceptions.SubstrateSdkException;
import com.salesforce.cantor.Events;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.salesforce.cantor.common.EventsPreconditions.*;

/**
 * Simplified EventsOnS3 implementation using Substrate SDK
 * 
 * Note: This is a streamlined version that removes AWS-specific features like:
 * - TransferManager for bulk uploads
 * - S3 Select for server-side query processing
 * - Complex buffering and batch upload mechanisms
 * - AWS-specific tagging and ACL features
 */
public class EventsOnS3 extends AbstractBaseS3Namespaceable implements Events {
    private static final Logger logger = LoggerFactory.getLogger(EventsOnS3.class);

    // cantor-events-<namespace>/<startTimestamp>-<endTimestamp>
    private static final String objectKeyPrefix = "cantor-events";
    
    // date formatter patterns for converting an event timestamp to a hierarchical directory structure
    private static final String directoryFormatterMinPattern = "yyyy/MM/dd/HH/mm";
    private static final String fileNameFormatterPattern = "yyyy-MM-dd_HH-mm";
    
    // monitors for synchronizing writes to namespaces
    private static final Map<String, Object> namespaceLocks = new ConcurrentHashMap<>();
    
    // json parser
    private final Gson parser = new GsonBuilder().create();

    public EventsOnS3(final BucketClient bucketClient) throws IOException {
        super(bucketClient, "events");
    }

    @Override
    public void store(final String namespace, final Collection<Event> batch) throws IOException {
        checkStore(namespace, batch);
        checkNamespace(namespace);
        try {
            doStore(namespace, batch);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception storing events to namespace: " + namespace, e);
            throw new IOException("exception storing events to namespace: " + namespace, e);
        }
    }

    @Override
    public List<Event> get(final String namespace,
                           final long startTimestampMillis,
                           final long endTimestampMillis,
                           final Map<String, String> metadataQuery,
                           final Map<String, String> dimensionsQuery,
                           final boolean includePayloads,
                           final boolean ascending,
                           final int limit) throws IOException {
        checkGet(namespace, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery);
        checkNamespace(namespace);
        try {
            return doGet(namespace,
                        startTimestampMillis,
                        endTimestampMillis,
                        (metadataQuery != null) ? metadataQuery : Collections.emptyMap(),
                        (dimensionsQuery != null) ? dimensionsQuery : Collections.emptyMap(),
                        includePayloads,
                        ascending,
                        limit);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting events from namespace: " + namespace, e);
            throw new IOException("exception getting events from namespace: " + namespace, e);
        }
    }

    @Override
    public Set<String> metadata(final String namespace,
                                final String metadataKey,
                                final long startTimestampMillis,
                                final long endTimestampMillis,
                                final Map<String, String> metadataQuery,
                                final Map<String, String> dimensionsQuery) throws IOException {
        checkMetadata(namespace, metadataKey, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery);
        checkNamespace(namespace);
        try {
            return doMetadata(namespace,
                    metadataKey,
                    startTimestampMillis,
                    endTimestampMillis,
                    (metadataQuery != null) ? metadataQuery : Collections.emptyMap(),
                    (dimensionsQuery != null) ? dimensionsQuery : Collections.emptyMap());
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting metadata from namespace: " + namespace, e);
            throw new IOException("exception getting metadata from namespace: " + namespace, e);
        }
    }

    @Override
    public List<Event> dimension(final String namespace,
                                 final String dimensionKey,
                                 final long startTimestampMillis,
                                 final long endTimestampMillis,
                                 final Map<String, String> metadataQuery,
                                 final Map<String, String> dimensionsQuery) throws IOException {
        checkDimension(namespace, dimensionKey, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery);
        checkNamespace(namespace);
        try {
            return doDimension(namespace,
                    dimensionKey,
                    startTimestampMillis,
                    endTimestampMillis,
                    (metadataQuery != null) ? metadataQuery : Collections.emptyMap(),
                    (dimensionsQuery != null) ? dimensionsQuery : Collections.emptyMap());
        } catch (final SubstrateSdkException e) {
            logger.warn("exception getting dimension from namespace: " + namespace, e);
            throw new IOException("exception getting dimension from namespace: " + namespace, e);
        }
    }

    @Override
    public void expire(final String namespace, final long endTimestampMillis) throws IOException {
        checkExpire(namespace, endTimestampMillis);
        checkNamespace(namespace);
        try {
            doExpire(namespace, endTimestampMillis);
        } catch (final SubstrateSdkException e) {
            logger.warn("exception expiring events from namespace: " + namespace, e);
            throw new IOException("exception expiring events from namespace: " + namespace, e);
        }
    }

    @Override
    protected String getObjectKeyPrefix(final String namespace) {
        return String.format("%s/%s", objectKeyPrefix, trim(namespace));
    }

    private void doStore(final String namespace, final Collection<Event> batch) throws IOException {
        // make sure there is a lock object for this namespace
        namespaceLocks.putIfAbsent(namespace, namespace);
        
        synchronized (namespaceLocks.get(namespace)) {
            // Group events by time period for efficient storage
            final Map<String, List<Event>> eventsByTimePeriod = groupEventsByTimePeriod(batch);
            
            for (final Map.Entry<String, List<Event>> entry : eventsByTimePeriod.entrySet()) {
                final String objectKey = entry.getKey();
                final List<Event> events = entry.getValue();
                
                // Convert events to JSON lines format
                final StringBuilder jsonLines = new StringBuilder();
                for (final Event event : events) {
                    jsonLines.append(this.parser.toJson(event)).append("\n");
                }
                
                // Store as a single object
                final byte[] content = jsonLines.toString().getBytes();
                final Map<String, String> metadata = Collections.singletonMap("Content-Type", "application/json");
                S3Utils.putObject(this.bucketClient, objectKey, new ByteArrayInputStream(content), metadata);
                
                logger.info("stored {} events to {}", events.size(), objectKey);
            }
        }
    }

    private Map<String, List<Event>> groupEventsByTimePeriod(final Collection<Event> batch) {
        final Map<String, List<Event>> grouped = new HashMap<>();
        final DateFormat directoryFormatter = new SimpleDateFormat(directoryFormatterMinPattern);
        final DateFormat fileNameFormatter = new SimpleDateFormat(fileNameFormatterPattern);
        
        for (final Event event : batch) {
            final String timePeriod = directoryFormatter.format(event.getTimestampMillis());
            final String fileName = fileNameFormatter.format(event.getTimestampMillis());
            final String objectKey = String.format("%s/%s/%s.json", 
                getObjectKeyPrefix("events"), timePeriod, fileName);
            
            grouped.computeIfAbsent(objectKey, k -> new ArrayList<>()).add(event);
        }
        
        return grouped;
    }

    private List<Event> doGet(final String namespace,
                              final long startTimestampMillis,
                              final long endTimestampMillis,
                              final Map<String, String> metadataQuery,
                              final Map<String, String> dimensionsQuery,
                              final boolean includePayloads,
                              final boolean ascending,
                              final int limit) throws IOException {
        
        final List<Event> results = new ArrayList<>();
        
        // Get all matching object keys for the time range
        final Set<String> matchingKeys = getMatchingKeys(namespace, startTimestampMillis, endTimestampMillis);
        
        for (final String objectKey : matchingKeys) {
            if (!objectKey.endsWith(".json")) {
                continue;
            }
            
            try {
                final byte[] content = S3Utils.getObjectBytes(this.bucketClient, objectKey);
                if (content == null) {
                    continue;
                }
                
                // Parse JSON lines and filter events
                final String jsonContent = new String(content);
                final String[] lines = jsonContent.split("\n");
                
                for (final String line : lines) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    try {
                        final Event event = this.parser.fromJson(line, Event.class);
                        
                        // Apply filters
                        if (matchesFilters(event, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery)) {
                            results.add(event);
                        }
                        
                        // Check limit
                        if (limit > 0 && results.size() >= limit) {
                            break;
                        }
                    } catch (final Exception e) {
                        logger.warn("Failed to parse event from line: " + line, e);
                    }
                }
                
                if (limit > 0 && results.size() >= limit) {
                    break;
                }
            } catch (final IOException e) {
                logger.warn("Failed to read events from object: " + objectKey, e);
            }
        }
        
        // Sort results
        sortEventsByTimestamp(results, ascending);
        
        // Apply final limit
        if (limit > 0 && results.size() > limit) {
            return results.subList(0, limit);
        }
        
        return results;
    }
    
    private boolean matchesFilters(final Event event,
                                   final long startTimestamp,
                                   final long endTimestamp,
                                   final Map<String, String> metadataQuery,
                                   final Map<String, String> dimensionsQuery) {
        
        // Time range filter
        if (event.getTimestampMillis() < startTimestamp || event.getTimestampMillis() > endTimestamp) {
            return false;
        }
        
        // Metadata filters (simplified - only exact matches)
        if (metadataQuery != null && !metadataQuery.isEmpty()) {
            final Map<String, String> eventMetadata = event.getMetadata();
            for (final Map.Entry<String, String> filter : metadataQuery.entrySet()) {
                final String value = eventMetadata.get(filter.getKey());
                if (!filter.getValue().equals(value)) {
                    return false;
                }
            }
        }
        
        // Dimension filters (simplified - only exact matches)
        if (dimensionsQuery != null && !dimensionsQuery.isEmpty()) {
            final Map<String, Double> eventDimensions = event.getDimensions();
            for (final Map.Entry<String, String> filter : dimensionsQuery.entrySet()) {
                try {
                    final Double expectedValue = Double.parseDouble(filter.getValue());
                    final Double actualValue = eventDimensions.get(filter.getKey());
                    if (!expectedValue.equals(actualValue)) {
                        return false;
                    }
                } catch (final NumberFormatException e) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private Set<String> doMetadata(final String namespace,
                                   final String metadataKey,
                                   final long startTimestampMillis,
                                   final long endTimestampMillis,
                                   final Map<String, String> metadataQuery,
                                   final Map<String, String> dimensionsQuery) throws IOException {
        
        final Set<String> results = new HashSet<>();
        final List<Event> events = doGet(namespace, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery, false, true, -1);
        
        for (final Event event : events) {
            final String value = event.getMetadata().get(metadataKey);
            if (value != null) {
                results.add(value);
            }
        }
        
        return results;
    }

    private List<Event> doDimension(final String namespace,
                                    final String dimensionKey,
                                    final long startTimestampMillis,
                                    final long endTimestampMillis,
                                    final Map<String, String> metadataQuery,
                                    final Map<String, String> dimensionsQuery) throws IOException {
        
        final List<Event> results = new ArrayList<>();
        final List<Event> events = doGet(namespace, startTimestampMillis, endTimestampMillis, metadataQuery, dimensionsQuery, false, true, -1);
        
        for (final Event event : events) {
            final Double value = event.getDimensions().get(dimensionKey);
            if (value != null) {
                // Create a new event with only the requested dimension
                final Map<String, Double> dimensions = Collections.singletonMap(dimensionKey, value);
                results.add(new Event(event.getTimestampMillis(), Collections.emptyMap(), dimensions));
            }
        }
        
        return results;
    }

    private void doExpire(final String namespace, final long endTimestampMillis) throws IOException {
        logger.info("expiring namespace '{}' with end timestamp of '{}'", namespace, endTimestampMillis);
        final Set<String> keys = getMatchingKeys(namespace, 0, endTimestampMillis);
        logger.info("expiring objects: {}", keys);
        
        if (!keys.isEmpty()) {
            S3Utils.deleteObjects(this.bucketClient, keys);
        }
    }

    private Set<String> getMatchingKeys(final String namespace, final long startTimestampMillis, final long endTimestampMillis) throws IOException {
        final DateFormat directoryFormatter = new SimpleDateFormat(directoryFormatterMinPattern);
        final Set<String> prefixes = new HashSet<>();
        
        // Generate time-based prefixes for the search range
        long current = startTimestampMillis;
        while (current <= endTimestampMillis) {
            final String prefix = String.format("%s/%s", getObjectKeyPrefix(namespace), directoryFormatter.format(current));
            prefixes.add(prefix);
            // Advance by 1 minute
            current += 60 * 1000;
        }
        
        final Set<String> matchingKeys = new HashSet<>();
        for (final String prefix : prefixes) {
            try {
                final Collection<String> keys = S3Utils.getKeys(this.bucketClient, prefix);
                matchingKeys.addAll(keys);
            } catch (final IOException e) {
                logger.warn("Failed to get keys for prefix: " + prefix, e);
            }
        }
        
        return matchingKeys;
    }

    private void sortEventsByTimestamp(final List<Event> events, final boolean ascending) {
        events.sort((event1, event2) -> {
            if (event1.getTimestampMillis() < event2.getTimestampMillis()) {
                return ascending ? -1 : 1;
            } else if (event1.getTimestampMillis() > event2.getTimestampMillis()) {
                return ascending ? 1 : -1;
            }
            return 0;
        });
    }
}
