package com.clickhouse.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import com.clickhouse.client.config.ClickHouseBufferingMode;
import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.client.config.ClickHouseOption;
import com.clickhouse.client.config.ClickHouseDefaults;
import com.clickhouse.client.config.ClickHouseSslMode;

/**
 * An immutable class holding client-specific options like
 * {@link ClickHouseCredentials} and {@link ClickHouseNodeSelector} etc.
 */
public class ClickHouseConfig implements Serializable {
    static final class ClientOptions {
        private static final ClientOptions INSTANCE = new ClientOptions();

        private final Map<String, ClickHouseOption> customOptions;

        private ClientOptions() {
            Map<String, ClickHouseOption> m = new LinkedHashMap<>();
            try {
                for (ClickHouseClient c : ClickHouseClientBuilder.loadClients()) {
                    Class<? extends ClickHouseOption> clazz = c.getOptionClass();
                    if (clazz == null || clazz == ClickHouseClientOption.class) {
                        continue;
                    }
                    for (ClickHouseOption o : clazz.getEnumConstants()) {
                        m.put(o.getKey(), o);
                    }
                }
            } catch (Exception e) {
                // ignore
            }

            customOptions = m.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(m);
        }
    }

    private static final long serialVersionUID = 7794222888859182491L;

    static final String PARAM_OPTION = "option";

    protected static final Map<ClickHouseOption, Serializable> mergeOptions(List<ClickHouseConfig> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ClickHouseOption, Serializable> options = new HashMap<>();
        List<ClickHouseConfig> cl = new ArrayList<>(list.size());
        for (ClickHouseConfig c : list) {
            if (c != null) {
                boolean duplicated = false;
                for (ClickHouseConfig conf : cl) {
                    if (conf == c) {
                        duplicated = true;
                        break;
                    }
                }

                if (duplicated) {
                    continue;
                }
                options.putAll(c.options);
                cl.add(c);
            }
        }

        return options;
    }

    protected static final ClickHouseCredentials mergeCredentials(List<ClickHouseConfig> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        ClickHouseCredentials credentials = null;
        for (ClickHouseConfig c : list) {
            if (c != null && c.credentials != null) {
                credentials = c.credentials;
                break;
            }
        }

        return credentials;
    }

    protected static final ClickHouseNodeSelector mergeNodeSelector(List<ClickHouseConfig> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        ClickHouseNodeSelector nodeSelector = null;
        for (ClickHouseConfig c : list) {
            if (c != null && c.nodeSelector != null) {
                nodeSelector = c.nodeSelector;
                break;
            }
        }

        return nodeSelector;
    }

    protected static final Object mergeMetricRegistry(List<ClickHouseConfig> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        Object metricRegistry = null;
        for (ClickHouseConfig c : list) {
            if (c != null && c.metricRegistry.isPresent()) {
                metricRegistry = c.metricRegistry.get();
                break;
            }
        }

        return metricRegistry;
    }

    /**
     * Converts given key-value pairs to a mutable map of corresponding
     * {@link ClickHouseOption}.
     *
     * @param props key-value pairs
     * @return non-null mutable map of client options
     */
    public static Map<ClickHouseOption, Serializable> toClientOptions(Map<?, ?> props) {
        Map<ClickHouseOption, Serializable> options = new HashMap<>();
        if (props != null && !props.isEmpty()) {
            Map<String, ClickHouseOption> customOptions = ClientOptions.INSTANCE.customOptions;
            for (Entry<?, ?> e : props.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) {
                    continue;
                }

                String key = e.getKey().toString();
                ClickHouseOption o = ClickHouseClientOption.fromKey(key);
                if (o == null) {
                    o = customOptions.get(key);
                }

                if (o != null) {
                    options.put(o, ClickHouseOption.fromString(e.getValue().toString(), o.getValueType()));
                }
            }
        }

        return options;
    }

    // common options optimized for read
    private final boolean async;
    private final boolean autoDiscovery;
    private final Map<String, String> customSettings; // serializable
    private final String clientName;
    private final boolean compressRequest;
    private final ClickHouseCompression compressAlgorithm;
    private final int compressLevel;
    private final boolean decompressResponse;
    private final ClickHouseCompression decompressAlgorithm;
    private final int decompressLevel;
    private final int connectionTimeout;
    private final String database;
    private final ClickHouseFormat format;
    private final int maxBufferSize;
    private final int bufferSize;
    private final int bufferQueueVariation;
    private final int readBufferSize;
    private final int writeBufferSize;
    private final int requestChunkSize;
    private final ClickHouseBufferingMode requestBuffering;
    private final ClickHouseBufferingMode responseBuffering;
    private final int maxExecutionTime;
    private final int maxQueuedBuffers;
    private final int maxQueuedRequests;
    private final long maxResultRows;
    private final int maxThreads;
    private final int nodeCheckInterval;
    private final int failover;
    private final int retry;
    private final boolean repeatOnSessionLock;
    private final boolean reuseValueWrapper;
    private final boolean serverInfo;
    private final TimeZone serverTimeZone;
    private final ClickHouseVersion serverVersion;
    private final int sessionTimeout;
    private final boolean sessionCheck;
    private final int socketTimeout;
    private final boolean ssl;
    private final ClickHouseSslMode sslMode;
    private final String sslRootCert;
    private final String sslCert;
    private final String sslKey;
    private final int transactionTimeout;
    private final boolean useBlockingQueue;
    private final boolean useObjectsInArray;
    private final boolean useNoProxy;
    private final boolean useServerTimeZone;
    private final boolean useServerTimeZoneForDates;
    private final TimeZone timeZoneForDate;
    private final TimeZone useTimeZone;

    // client specific options
    private final Map<ClickHouseOption, Serializable> options;
    private final ClickHouseCredentials credentials;
    private final transient Optional<Object> metricRegistry;

    // node selector - pick only interested nodes from given list
    private final ClickHouseNodeSelector nodeSelector;

    /**
     * Construct a new configuration by consolidating given ones.
     *
     * @param configs list of configuration
     */
    public ClickHouseConfig(ClickHouseConfig... configs) {
        this(configs == null || configs.length == 0 ? Collections.emptyList() : Arrays.asList(configs));
    }

    /**
     * Construct a new configuration by consolidating given ones.
     *
     * @param configs list of configuration
     */
    public ClickHouseConfig(List<ClickHouseConfig> configs) {
        this(mergeOptions(configs), mergeCredentials(configs), mergeNodeSelector(configs),
                mergeMetricRegistry(configs));
    }

    /**
     * Default contructor.
     *
     * @param options        generic options
     * @param credentials    default credential
     * @param nodeSelector   node selector
     * @param metricRegistry metric registry
     */
    public ClickHouseConfig(Map<ClickHouseOption, Serializable> options, ClickHouseCredentials credentials,
            ClickHouseNodeSelector nodeSelector, Object metricRegistry) {
        this.options = new HashMap<>();
        if (options != null) {
            this.options.putAll(options);
        }

        this.async = (boolean) getOption(ClickHouseClientOption.ASYNC, ClickHouseDefaults.ASYNC);
        this.autoDiscovery = getBoolOption(ClickHouseClientOption.AUTO_DISCOVERY);
        this.customSettings = ClickHouseOption.toKeyValuePairs(getStrOption(ClickHouseClientOption.CUSTOM_SETTINGS));
        this.clientName = getStrOption(ClickHouseClientOption.CLIENT_NAME);
        this.compressRequest = getBoolOption(ClickHouseClientOption.DECOMPRESS);
        this.compressAlgorithm = getOption(ClickHouseClientOption.DECOMPRESS_ALGORITHM, ClickHouseCompression.class);
        this.compressLevel = getIntOption(ClickHouseClientOption.DECOMPRESS_LEVEL);
        this.decompressResponse = getBoolOption(ClickHouseClientOption.COMPRESS);
        this.decompressAlgorithm = getOption(ClickHouseClientOption.COMPRESS_ALGORITHM, ClickHouseCompression.class);
        this.decompressLevel = getIntOption(ClickHouseClientOption.COMPRESS_LEVEL);
        this.connectionTimeout = getIntOption(ClickHouseClientOption.CONNECTION_TIMEOUT);
        this.database = (String) getOption(ClickHouseClientOption.DATABASE, ClickHouseDefaults.DATABASE);
        this.format = (ClickHouseFormat) getOption(ClickHouseClientOption.FORMAT, ClickHouseDefaults.FORMAT);
        this.maxBufferSize = ClickHouseUtils.getBufferSize(getIntOption(ClickHouseClientOption.MAX_BUFFER_SIZE), -1,
                -1);
        this.bufferSize = getIntOption(ClickHouseClientOption.BUFFER_SIZE);
        this.bufferQueueVariation = getIntOption(ClickHouseClientOption.BUFFER_QUEUE_VARIATION);
        this.readBufferSize = getIntOption(ClickHouseClientOption.READ_BUFFER_SIZE);
        this.writeBufferSize = getIntOption(ClickHouseClientOption.WRITE_BUFFER_SIZE);
        this.requestChunkSize = getIntOption(ClickHouseClientOption.REQUEST_CHUNK_SIZE);
        this.requestBuffering = (ClickHouseBufferingMode) getOption(ClickHouseClientOption.REQUEST_BUFFERING,
                ClickHouseDefaults.BUFFERING);
        this.responseBuffering = (ClickHouseBufferingMode) getOption(ClickHouseClientOption.RESPONSE_BUFFERING,
                ClickHouseDefaults.BUFFERING);
        this.maxExecutionTime = getIntOption(ClickHouseClientOption.MAX_EXECUTION_TIME);
        this.maxQueuedBuffers = getIntOption(ClickHouseClientOption.MAX_QUEUED_BUFFERS);
        this.maxQueuedRequests = getIntOption(ClickHouseClientOption.MAX_QUEUED_REQUESTS);
        this.maxResultRows = getLongOption(ClickHouseClientOption.MAX_RESULT_ROWS);
        this.maxThreads = getIntOption(ClickHouseClientOption.MAX_THREADS_PER_CLIENT);
        this.nodeCheckInterval = getIntOption(ClickHouseClientOption.NODE_CHECK_INTERVAL);
        this.failover = getIntOption(ClickHouseClientOption.FAILOVER);
        this.retry = getIntOption(ClickHouseClientOption.RETRY);
        this.repeatOnSessionLock = getBoolOption(ClickHouseClientOption.REPEAT_ON_SESSION_LOCK);
        this.reuseValueWrapper = getBoolOption(ClickHouseClientOption.REUSE_VALUE_WRAPPER);
        this.serverInfo = !ClickHouseChecker.isNullOrBlank(getStrOption(ClickHouseClientOption.SERVER_TIME_ZONE))
                && !ClickHouseChecker.isNullOrBlank(getStrOption(ClickHouseClientOption.SERVER_VERSION));
        this.serverTimeZone = TimeZone.getTimeZone(
                (String) getOption(ClickHouseClientOption.SERVER_TIME_ZONE, ClickHouseDefaults.SERVER_TIME_ZONE));
        this.serverVersion = ClickHouseVersion
                .of((String) getOption(ClickHouseClientOption.SERVER_VERSION, ClickHouseDefaults.SERVER_VERSION));
        this.sessionTimeout = getIntOption(ClickHouseClientOption.SESSION_TIMEOUT);
        this.sessionCheck = getBoolOption(ClickHouseClientOption.SESSION_CHECK);
        this.socketTimeout = getIntOption(ClickHouseClientOption.SOCKET_TIMEOUT);
        this.ssl = getBoolOption(ClickHouseClientOption.SSL);
        this.sslMode = getOption(ClickHouseClientOption.SSL_MODE, ClickHouseSslMode.class);
        this.sslRootCert = getStrOption(ClickHouseClientOption.SSL_ROOT_CERTIFICATE);
        this.sslCert = getStrOption(ClickHouseClientOption.SSL_CERTIFICATE);
        this.sslKey = getStrOption(ClickHouseClientOption.SSL_KEY);
        this.transactionTimeout = getIntOption(ClickHouseClientOption.TRANSACTION_TIMEOUT);
        this.useBlockingQueue = getBoolOption(ClickHouseClientOption.USE_BLOCKING_QUEUE);
        this.useObjectsInArray = getBoolOption(ClickHouseClientOption.USE_OBJECTS_IN_ARRAYS);
        this.useNoProxy = getBoolOption(ClickHouseClientOption.USE_NO_PROXY);
        this.useServerTimeZone = getBoolOption(ClickHouseClientOption.USE_SERVER_TIME_ZONE);
        this.useServerTimeZoneForDates = getBoolOption(ClickHouseClientOption.USE_SERVER_TIME_ZONE_FOR_DATES);

        String timeZone = getStrOption(ClickHouseClientOption.USE_TIME_ZONE);
        TimeZone tz = ClickHouseChecker.isNullOrBlank(timeZone) ? TimeZone.getDefault()
                : TimeZone.getTimeZone(timeZone);
        this.useTimeZone = this.useServerTimeZone ? this.serverTimeZone : tz;
        this.timeZoneForDate = this.useServerTimeZoneForDates ? this.useTimeZone : null;

        if (credentials == null) {
            this.credentials = ClickHouseCredentials.fromUserAndPassword(getStrOption(ClickHouseDefaults.USER),
                    getStrOption(ClickHouseDefaults.PASSWORD));
        } else {
            this.credentials = credentials;
        }
        this.metricRegistry = Optional.ofNullable(metricRegistry);
        this.nodeSelector = nodeSelector == null ? ClickHouseNodeSelector.EMPTY : nodeSelector;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isAutoDiscovery() {
        return autoDiscovery;
    }

    public Map<String, String> getCustomSettings() {
        return customSettings;
    }

    public String getClientName() {
        return clientName;
    }

    /**
     * Checks if server response is compressed or not.
     *
     * @return true if server response is compressed; false otherwise
     */
    public boolean isResponseCompressed() {
        return decompressResponse;
    }

    /**
     * Gets server response compress algorithm. When {@link #isResponseCompressed()}
     * is {@code false}, this will return {@link ClickHouseCompression#NONE}.
     *
     * @return non-null compress algorithm
     */
    public ClickHouseCompression getResponseCompressAlgorithm() {
        return decompressResponse ? decompressAlgorithm : ClickHouseCompression.NONE;
    }

    /**
     * Gets input compress level. When {@link #isResponseCompressed()} is
     * {@code false}, this will return {@code 0}.
     *
     * @return compress level
     */
    public int getResponseCompressLevel() {
        return decompressResponse ? decompressLevel : 0;
    }

    /**
     * Checks if server response should be compressed or not.
     *
     * @return true if server response is compressed; false otherwise
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #isResponseCompressed()} instead
     */
    @Deprecated
    public boolean isCompressServerResponse() {
        return decompressResponse;
    }

    /**
     * Gets compress algorithm for server response.
     *
     * @return compress algorithm for server response
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #getResponseCompressAlgorithm()} instead
     */
    @Deprecated
    public ClickHouseCompression getCompressAlgorithmForServerResponse() {
        return decompressAlgorithm;
    }

    /**
     * Gets compress level for server response.
     *
     * @return compress level
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #getResponseCompressLevel()} instead
     */
    @Deprecated
    public int getCompressLevelForServerResponse() {
        return decompressLevel;
    }

    /**
     * Checks if client's output, aka. client request, should be compressed or not.
     *
     * @return true if client request should be compressed; false otherwise
     */
    public boolean isRequestCompressed() {
        return compressRequest;
    }

    /**
     * Gets input compress algorithm. When {@link #isRequestCompressed()} is
     * {@code false}, this will return {@link ClickHouseCompression#NONE}.
     *
     * @return non-null compress algorithm
     */
    public ClickHouseCompression getRequestCompressAlgorithm() {
        return compressRequest ? compressAlgorithm : ClickHouseCompression.NONE;
    }

    /**
     * Gets input compress level. When {@link #isRequestCompressed()} is
     * {@code false}, this will return {@code 0}.
     *
     * @return compress level
     */
    public int getRequestCompressLevel() {
        return compressRequest ? compressLevel : 0;
    }

    /**
     * Checks if client request should be compressed or not.
     *
     * @return true if server needs to decompress client request; false otherwise
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #isRequestCompressed()} instead
     */
    @Deprecated
    public boolean isDecompressClientRequet() {
        return compressRequest;
    }

    /**
     * Gets compress algorithm for client request.
     *
     * @return compress algorithm for client request
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #getRequestCompressAlgorithm()} instead
     */
    @Deprecated
    public ClickHouseCompression getDecompressAlgorithmForClientRequest() {
        return decompressAlgorithm;
    }

    /**
     * Gets compress level for client request.
     *
     * @return compress level
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #getRequestCompressLevel()} instead
     */
    @Deprecated
    public int getDecompressLevelForClientRequest() {
        return decompressLevel;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getDatabase() {
        return database;
    }

    public ClickHouseFormat getFormat() {
        return format;
    }

    public int getNodeCheckInterval() {
        return nodeCheckInterval;
    }

    /**
     * Gets max buffer size in byte can be used for streaming.
     *
     * @return max buffer size in byte
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * Gets default buffer size in byte for both read and write.
     *
     * @return default buffer size in byte
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Gets number of times the buffer queue is filled up before
     * increasing capacity of buffer queue. Zero or negative value means the queue
     * length is fixed.
     *
     * @return variation
     */
    public int getBufferQueueVariation() {
        return bufferQueueVariation;
    }

    /**
     * Gets read buffer size in byte.
     *
     * @return read buffer size in byte
     */
    public int getReadBufferSize() {
        return ClickHouseUtils.getBufferSize(readBufferSize, getBufferSize(), getMaxBufferSize());
    }

    /**
     * Gets write buffer size in byte.
     *
     * @return write buffer size in byte
     */
    public int getWriteBufferSize() {
        return ClickHouseUtils.getBufferSize(writeBufferSize, getBufferSize(), getMaxBufferSize());
    }

    /**
     * Gets request chunk size.
     *
     * @return request chunk size
     */
    public int getRequestChunkSize() {
        return ClickHouseUtils.getBufferSize(requestChunkSize, getWriteBufferSize(), getMaxBufferSize());
    }

    /**
     * Gets request buffering mode.
     *
     * @return request buffering mode
     */
    public ClickHouseBufferingMode getRequestBuffering() {
        return requestBuffering;
    }

    /**
     * Gets response buffering mode.
     *
     * @return response buffering mode
     */
    public ClickHouseBufferingMode getResponseBuffering() {
        return responseBuffering;
    }

    public int getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public int getMaxQueuedBuffers() {
        return maxQueuedBuffers;
    }

    public int getMaxQueuedRequests() {
        return maxQueuedRequests;
    }

    public long getMaxResultRows() {
        return maxResultRows;
    }

    public int getMaxThreadsPerClient() {
        return maxThreads;
    }

    public int getFailover() {
        return failover;
    }

    public int getRetry() {
        return retry;
    }

    public boolean isRepeatOnSessionLock() {
        return repeatOnSessionLock;
    }

    /**
     * Checks whether retry is enabled or not.
     *
     * @return true if retry is enabled; false otherwise
     * @deprecated will be removed in v0.3.3, please use
     *             {@link #getRetry()} instead
     */
    @Deprecated
    public boolean isRetry() {
        return retry > 0;
    }

    public boolean isReuseValueWrapper() {
        return reuseValueWrapper;
    }

    /**
     * Checks whether we got all server information(e.g. timezone and version).
     *
     * @return true if we got all server information; false otherwise
     */
    public boolean hasServerInfo() {
        return serverInfo;
    }

    public TimeZone getServerTimeZone() {
        return serverTimeZone;
    }

    public ClickHouseVersion getServerVersion() {
        return serverVersion;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public boolean isSessionCheck() {
        return sessionCheck;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public boolean isSsl() {
        return ssl;
    }

    public ClickHouseSslMode getSslMode() {
        return sslMode;
    }

    public String getSslRootCert() {
        return sslRootCert;
    }

    public String getSslCert() {
        return sslCert;
    }

    public String getSslKey() {
        return sslKey;
    }

    public int getTransactionTimeout() {
        return transactionTimeout < 1 ? sessionTimeout : transactionTimeout;
    }

    public boolean isUseBlockingQueue() {
        return useBlockingQueue;
    }

    public boolean isUseObjectsInArray() {
        return useObjectsInArray;
    }

    public boolean isUseNoProxy() {
        return useNoProxy;
    }

    public boolean isUseServerTimeZone() {
        return useServerTimeZone;
    }

    public boolean isUseServerTimeZoneForDates() {
        return useServerTimeZoneForDates;
    }

    /**
     * Gets time zone for date values.
     *
     * @return time zone, could be null when {@code use_server_time_zone_for_date}
     *         is set to {@code false}.
     */
    public TimeZone getTimeZoneForDate() {
        return timeZoneForDate;
    }

    /**
     * Gets preferred time zone. When {@link #isUseServerTimeZone()} is
     * {@code true}, this returns same time zone as {@link #getServerTimeZone()}.
     *
     * @return non-null preferred time zone
     */
    public TimeZone getUseTimeZone() {
        return useTimeZone;
    }

    public ClickHouseCredentials getDefaultCredentials() {
        return this.credentials;
    }

    public Optional<Object> getMetricRegistry() {
        return this.metricRegistry;
    }

    public ClickHouseNodeSelector getNodeSelector() {
        return this.nodeSelector;
    }

    public List<ClickHouseProtocol> getPreferredProtocols() {
        return this.nodeSelector.getPreferredProtocols();
    }

    public Set<String> getPreferredTags() {
        return this.nodeSelector.getPreferredTags();
    }

    public Map<ClickHouseOption, Serializable> getAllOptions() {
        return Collections.unmodifiableMap(this.options);
    }

    /**
     * Gets typed option value. {@link ClickHouseOption#getEffectiveDefaultValue}
     * will be called when the option is undefined.
     *
     * @param <T>       type of option value, must be serializable
     * @param option    non-null option to lookup
     * @param valueType non-null type of option value, must be serializable
     * @return typed value
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getOption(ClickHouseOption option, Class<T> valueType) {
        if (ClickHouseChecker.nonNull(option, PARAM_OPTION).getValueType() != ClickHouseChecker.nonNull(valueType,
                "valueType")) {
            throw new IllegalArgumentException(
                    "Cannot convert value from type " + option.getValueType() + " to " + valueType);
        }

        T value = (T) options.get(option);
        return value != null ? value : (T) option.getEffectiveDefaultValue();
    }

    /**
     * Gets option value.
     *
     * @param option        non-null option to lookup
     * @param defaultConfig optional default config to retrieve default value
     * @return option value
     */
    public Serializable getOption(ClickHouseOption option, ClickHouseConfig defaultConfig) {
        return this.options.getOrDefault(ClickHouseChecker.nonNull(option, PARAM_OPTION),
                defaultConfig == null ? option.getEffectiveDefaultValue() : defaultConfig.getOption(option));
    }

    /**
     * Gets option value.
     *
     * @param option       non-null option to lookup
     * @param defaultValue optional default value
     * @return option value
     */
    public Serializable getOption(ClickHouseOption option, ClickHouseDefaults defaultValue) {
        return this.options.getOrDefault(ClickHouseChecker.nonNull(option, PARAM_OPTION),
                defaultValue == null ? option.getEffectiveDefaultValue() : defaultValue.getEffectiveDefaultValue());
    }

    /**
     * Shortcut of {@link #getOption(ClickHouseOption, ClickHouseDefaults)}.
     *
     * @param option non-null option to lookup
     * @return option value
     */
    public Serializable getOption(ClickHouseOption option) {
        return getOption(option, (ClickHouseDefaults) null);
    }

    /**
     * Shortcut of {@code getOption(option, Boolean.class)}.
     *
     * @param option non-null option to lookup
     * @return boolean value of the given option
     */
    public boolean getBoolOption(ClickHouseOption option) {
        return getOption(option, Boolean.class);
    }

    /**
     * Shortcut of {@code getOption(option, Integer.class)}.
     *
     * @param option non-null option to lookup
     * @return int value of the given option
     */
    public int getIntOption(ClickHouseOption option) {
        return getOption(option, Integer.class);
    }

    /**
     * Shortcut of {@code getOption(option, Long.class)}.
     *
     * @param option non-null option to lookup
     * @return long value of the given option
     */
    public long getLongOption(ClickHouseOption option) {
        return getOption(option, Long.class);
    }

    /**
     * Shortcut of {@code getOption(option, String.class)}.
     *
     * @param option non-null option to lookup
     * @return String value of the given option
     */
    public String getStrOption(ClickHouseOption option) {
        return getOption(option, String.class);
    }

    /**
     * Test whether a given option is configured or not.
     *
     * @param option option to test
     * @return true if the option is configured; false otherwise
     */
    public boolean hasOption(ClickHouseOption option) {
        return option != null && this.options.containsKey(option);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, credentials, metricRegistry.orElse(null), nodeSelector);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ClickHouseConfig other = (ClickHouseConfig) obj;
        return Objects.equals(options, other.options) && Objects.equals(credentials, other.credentials)
                && Objects.equals(metricRegistry.orElse(null), other.metricRegistry.orElse(null))
                && Objects.equals(nodeSelector, other.nodeSelector);
    }
}
