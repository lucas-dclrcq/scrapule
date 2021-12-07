package org.ldclrcq.document.scrapper.scaleway;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

public class CitizConnectorConfig extends AbstractConfig {

    public CitizConnectorConfig(final Map<?, ?> originalProps) {
        super(CONFIG_DEF, originalProps);
    }

    public static final String CITIZ_USERNAME = "first.required.param";
    private static final String CITIZ_USERNAME_DOC = "This is the 1st required parameter";

    public static final String CITIZ_PASSWORD = "second.required.param";
    private static final String CITIZ_PASSWORD_DOC = "This is the 2nd required parameter";

    public static final String CITIZ_PROVIDER_ID = "first.nonrequired.param";
    private static final String CITIZ_PROVIDER_ID_DOC = "This is the 1st non-required parameter";

    public static final String MONITOR_THREAD_TIMEOUT_CONFIG = "monitor.thread.timeout";
    private static final String MONITOR_THREAD_TIMEOUT_DOC = "Timeout used by the monitoring thread";
    private static final int MONITOR_THREAD_TIMEOUT_DEFAULT = 10000;

    public static final ConfigDef CONFIG_DEF = createConfigDef();

    private static ConfigDef createConfigDef() {
        ConfigDef configDef = new ConfigDef();
        addParams(configDef);
        return configDef;
    }

    private static void addParams(final ConfigDef configDef) {
        configDef.define(
                        CITIZ_USERNAME,
                        Type.STRING,
                        Importance.HIGH,
                        CITIZ_USERNAME_DOC)
                .define(
                        CITIZ_PASSWORD,
                        Type.STRING,
                        Importance.HIGH,
                        CITIZ_PASSWORD_DOC)
                .define(
                        CITIZ_PROVIDER_ID,
                        Type.STRING,
                        Importance.HIGH,
                        CITIZ_PROVIDER_ID_DOC)
                .define(
                        MONITOR_THREAD_TIMEOUT_CONFIG,
                        Type.INT,
                        MONITOR_THREAD_TIMEOUT_DEFAULT,
                        Importance.LOW,
                        MONITOR_THREAD_TIMEOUT_DOC);
    }

}