/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.resource.queue.pulsar;

import org.apache.inlong.manager.common.util.HttpUtils;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.sink.queue.pulsar.PulsarLookupTopicInfo;
import org.apache.inlong.manager.pojo.sink.queue.pulsar.PulsarNamespacePolicies;
import org.apache.inlong.manager.pojo.sink.queue.pulsar.PulsarPartitionedInternalStats;
import org.apache.inlong.manager.pojo.sink.queue.pulsar.PulsarTenantInfo;
import org.apache.inlong.manager.pojo.sink.queue.pulsar.PulsarTopicMetadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pulsar connection utils
 */
@Slf4j
public class PulsarUtils {

    private PulsarUtils() {
    }

    public static final String QUERY_CLUSTERS_PATH = "/admin/v2/clusters";
    public static final String QUERY_BROKERS_PATH = "/admin/v2/brokers";
    public static final String QUERY_TENANTS_PATH = "/admin/v2/tenants";
    public static final String QUERY_NAMESPACE_PATH = "/admin/v2/namespaces";
    public static final String QUERY_PERSISTENT_PATH = "/admin/v2/persistent";
    public static final String LOOKUP_TOPIC_PATH = "/lookup/v2/topic";

    private static final Gson GSON = new GsonBuilder().create(); // thread safe

    /**
     * Get http headers by token.
     *
     * @param token pulsar token info
     * @return add http headers for token info
     */
    private static HttpHeaders getHttpHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotEmpty(token)) {
            headers.add("Authorization", "Bearer " + token);
        }
        return headers;
    }

    /**
     * Get pulsar cluster info list.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @return list of pulsar cluster infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarClusters(RestTemplate restTemplate, PulsarClusterInfo clusterInfo)
            throws Exception {
        final String url = clusterInfo.getAdminUrl() + QUERY_CLUSTERS_PATH;
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Get the list of active brokers.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @return list of pulsar broker infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarBrokers(RestTemplate restTemplate, PulsarClusterInfo clusterInfo)
            throws Exception {
        final String url = clusterInfo.getAdminUrl() + QUERY_BROKERS_PATH;
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Get pulsar tenant info list.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @return list of pulsar tenant infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarTenants(RestTemplate restTemplate, PulsarClusterInfo clusterInfo)
            throws Exception {
        final String url = clusterInfo.getAdminUrl() + QUERY_TENANTS_PATH;
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Get pulsar namespace info list.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param tenant pulsar tenant name
     * @return list of pulsar namespace infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarNamespaces(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String tenant) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_NAMESPACE_PATH + "/" + tenant;
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Create a new pulsar tenant.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param tenant pulsar tenant name
     * @param tenantInfo  pulsar tenant info
     * @throws Exception any exception if occurred
     */
    public static void createTenant(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String tenant,
            PulsarTenantInfo tenantInfo) throws Exception {
        final String url = clusterInfo.getAdminUrl() + QUERY_TENANTS_PATH + "/" + tenant;
        HttpHeaders headers = getHttpHeaders(clusterInfo.getToken());
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        String param = GSON.toJson(tenantInfo);
        HttpUtils.request(restTemplate, url, HttpMethod.PUT, param, headers);
    }

    /**
     * Creates a new pulsar namespace with the specified policies.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param namespaceName pulsar namespace name
     * @param policies pulsar namespace policies info
     * @throws Exception any exception if occurred
     */
    public static void createNamespace(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String namespaceName,
            PulsarNamespacePolicies policies) throws Exception {
        final String url = clusterInfo.getAdminUrl() + QUERY_NAMESPACE_PATH + "/" + namespaceName;
        HttpHeaders headers = getHttpHeaders(clusterInfo.getToken());
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        String param = GSON.toJson(policies);
        param = param.replaceAll("messageTtlInSeconds", "message_ttl_in_seconds")
                .replaceAll("retentionPolicies", "retention_policies");
        HttpUtils.request(restTemplate, url, HttpMethod.PUT, param, headers);
    }

    /**
     * Get the list of topics under a namespace.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param tenant pulsar tenant name
     * @param namespace pulsar namespace name
     * @return list of pulsar topic infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarTopics(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String tenant,
            String namespace) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + tenant + "/" + namespace;
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Get the list of partitioned topics under a namespace.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param tenant pulsar tenant name
     * @param namespace pulsar namespace name
     * @return list of pulsar partitioned topic infos
     * @throws Exception any exception if occurred
     */
    public static List<String> getPulsarPartitionedTopics(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String tenant, String namespace) throws Exception {
        String url =
                clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + tenant + "/" + namespace + "/partitioned";
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Create a non-partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void createNonPartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath;
        HttpUtils.request(restTemplate, url, HttpMethod.PUT, null, getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Create a partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void createPartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath, Integer numPartitions) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/partitions";
        HttpUtils.request(restTemplate, url, HttpMethod.PUT, numPartitions.toString(),
                getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Get the stats-internal for the partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @return pulsar internal stat info of partitioned topic
     * @throws Exception any exception if occurred
     */
    public static PulsarPartitionedInternalStats getPulsarStatsPartitionedTopics(RestTemplate restTemplate,
            PulsarClusterInfo clusterInfo, String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/partitioned-internalStats";
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                PulsarPartitionedInternalStats.class);
    }

    /**
     * Get partitioned topic metadata.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @return pulsar topic metadata info
     * @throws Exception any exception if occurred
     */
    public static PulsarTopicMetadata getPulsarPartitionedTopicMetadata(RestTemplate restTemplate,
            PulsarClusterInfo clusterInfo, String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/partitions";
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                PulsarTopicMetadata.class);
    }

    /**
     * Delete a topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void deleteNonPartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath;
        HttpUtils.request(restTemplate, url, HttpMethod.DELETE, null, getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Force delete a topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void forceDeleteNonPartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath;
        Map<String, Boolean> uriVariables = new HashMap<>();
        uriVariables.put("force", true);
        HttpUtils.request(restTemplate, url, HttpMethod.DELETE, uriVariables, getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Delete a partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void deletePartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/partitions";
        HttpUtils.request(restTemplate, url, HttpMethod.DELETE, null, getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Force delete a partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @throws Exception any exception if occurred
     */
    public static void forceDeletePartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/partitions";
        Map<String, Boolean> uriVariables = new HashMap<>();
        uriVariables.put("force", true);
        HttpUtils.request(restTemplate, url, HttpMethod.DELETE, uriVariables, getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Delete a partitioned or non-partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @param isPartitioned pulsar is partitioned topic
     * @throws Exception any exception if occurred
     */
    public static void deleteTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String topicPath,
            boolean isPartitioned) throws Exception {
        if (isPartitioned) {
            deletePartitionedTopic(restTemplate, clusterInfo, topicPath);
        } else {
            deleteNonPartitionedTopic(restTemplate, clusterInfo, topicPath);
        }
    }

    /**
     * Force delete a partitioned or non-partitioned topic.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @param isPartitioned pulsar is partitioned topic
     * @throws Exception any exception if occurred
     */
    public static void forceDeleteTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String topicPath,
            boolean isPartitioned)
            throws Exception {
        if (isPartitioned) {
            forceDeletePartitionedTopic(restTemplate, clusterInfo, topicPath);
        } else {
            forceDeleteNonPartitionedTopic(restTemplate, clusterInfo, topicPath);
        }
    }

    /**
     * lookup persistent topic info.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @return pulsar broker url
     * @throws Exception any exception if occurred
     */
    public static String lookupTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String topicPath)
            throws Exception {
        String url = clusterInfo.getAdminUrl() + LOOKUP_TOPIC_PATH + "/persistent/" + topicPath;
        PulsarLookupTopicInfo topicInfo = HttpUtils.request(restTemplate, url, HttpMethod.GET, null,
                getHttpHeaders(clusterInfo.getToken()), PulsarLookupTopicInfo.class);
        return topicInfo.getBrokerUrl();
    }

    /**
     * lookup persistent partitioned topic info.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @return map of partitioned topic info
     * @throws Exception any exception if occurred
     */
    public static Map<String, String> lookupPartitionedTopic(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + LOOKUP_TOPIC_PATH + "/persistent/" + topicPath;
        PulsarTopicMetadata metadata = getPulsarPartitionedTopicMetadata(restTemplate, clusterInfo, topicPath);
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < metadata.getPartitions(); i++) {
            String partitionTopicName = topicPath + "-partition-" + i;
            String partitionUrl = clusterInfo.getAdminUrl() + LOOKUP_TOPIC_PATH + "/persistent/" + partitionTopicName;
            PulsarLookupTopicInfo topicInfo = HttpUtils.request(restTemplate, partitionUrl, HttpMethod.GET, null,
                    getHttpHeaders(clusterInfo.getToken()), PulsarLookupTopicInfo.class);
            map.put(partitionTopicName, topicInfo.getBrokerUrl());
        }
        return map;
    }

    /**
     * Get topic subscriptions.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @return list of pulsar topic subscription info
     * @throws Exception any exception if occurred
     */
    public static List<String> getSubscriptions(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPath) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/subscriptions";
        return HttpUtils.request(restTemplate, url, HttpMethod.GET, null, getHttpHeaders(clusterInfo.getToken()),
                ArrayList.class);
    }

    /**
     * Create a topic subscription.
     *
     * @param restTemplate  spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPath pulsar topic path
     * @param subscription pulsar topic subscription info
     * @throws Exception any exception if occurred
     */
    public static void createSubscription(RestTemplate restTemplate, PulsarClusterInfo clusterInfo, String topicPath,
            String subscription) throws Exception {
        String url = clusterInfo.getAdminUrl() + QUERY_PERSISTENT_PATH + "/" + topicPath + "/subscriptions/" + subscription;
        HttpUtils.request(restTemplate, url, HttpMethod.PUT, "latest", getHttpHeaders(clusterInfo.getToken()));
    }

    /**
     * Examine a pulsar message.
     *
     * @param restTemplate spring framework RestTemplate
     * @param clusterInfo pulsar cluster info
     * @param topicPartition  pulsar topic partition info
     * @param messageType pulsar message type info
     * @param messagePosition pulsar message position info
     * @return spring framework HttpEntity
     * @throws Exception any exception if occurred
     */
    public static ResponseEntity<byte[]> examineMessage(RestTemplate restTemplate, PulsarClusterInfo clusterInfo,
            String topicPartition, String messageType, int messagePosition) throws Exception {
        StringBuilder urlBuilder = new StringBuilder().append(clusterInfo.getAdminUrl())
                .append(QUERY_PERSISTENT_PATH)
                .append("/")
                .append(topicPartition)
                .append("/examinemessage")
                .append("?initialPosition=")
                .append(messageType)
                .append("&messagePosition=")
                .append(messagePosition);
        return restTemplate.exchange(urlBuilder.toString(), HttpMethod.GET,
                new HttpEntity<>(getHttpHeaders(clusterInfo.getToken())), byte[].class);
    }
}
