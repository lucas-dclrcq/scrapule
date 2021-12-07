package org.ldclrcq.document.scrapper.scaleway;

import okhttp3.*;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

public class CitizSourceTask extends SourceTask {
    private final static String AUTH_URI = "https://service.citiz.fr/citiz/api/customer/login";

    private CitizConnectorConfig config;
    private OkHttpClient client;

    @Override
    public String version() {
        return null;
    }

    @Override
    public void start(Map<String, String> properties) {
        config = new CitizConnectorConfig(properties);
        client = new OkHttpClient();
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        final var username = config.getString(CitizConnectorConfig.CITIZ_USERNAME);
        final var password = config.getString(CitizConnectorConfig.CITIZ_PASSWORD);
        final var providerId = config.getInt(CitizConnectorConfig.CITIZ_PROVIDER_ID);

        try {
            final var offset = context.offsetStorageReader().offset(Collections.singletonMap("", ""));

            if (offset != null) {
                final var lastRecordedOffset = offset.get("");

                if (lastRecordedOffset != null && !(lastRecordedOffset instanceof Integer)) {
                    throw new ConnectException("Offset position of incorrect type");
                }

                int invoiceOffset = 0;

                if (lastRecordedOffset != null) {
                    //log.debug("Found previous offset, trying to skip to ");
                    invoiceOffset = (Integer) lastRecordedOffset;
                }

                final List<SourceRecord> records = new ArrayList<>();
                final var authInfo = authenticate(username, password, providerId);
                final var availableInvoiceIds = fetchAvailableInvoiceIds(authInfo, providerId, invoiceOffset, 0);

                for (var invoiceId : availableInvoiceIds) {
                    final byte[] fileBytes = downloadInvoice(authInfo, invoiceId);

                    final var key = new JSONObject()
                            .put("scrapperId", "citiz")
                            .put("filename", "citiz_invoice_" + invoiceId + ".pdf");

                    final var sourceRecord = new SourceRecord(
                            offsetKey(""),
                            offsetValue(invoiceId),
                            "",
                            null,
                            null,
                            key.toString(),
                            null,
                            fileBytes,
                            System.currentTimeMillis());


                    records.add(sourceRecord);
                }

                return records;
            }
        } catch (ConnectException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.EMPTY_LIST;
    }

    private byte[] downloadInvoice(CitizAuthInfo authInfo, Integer invoiceId) throws IOException {
        final var downloadUrl = "https://service.citiz.fr/citiz/api/customer/97601/invoices/%d?token=%s&providerId=16".formatted(invoiceId, authInfo.token());
        final var downloadRequest = new Request.Builder()
                .url(downloadUrl)
                .get()
                .build();

        final var downloadCall = client.newCall(downloadRequest);
        final var response = downloadCall.execute();
        return response.body().bytes();
    }

    private List<Integer> fetchAvailableInvoiceIds(CitizAuthInfo authInfo, int providerId, int invoiceOffset, int batchSize) throws IOException {
        final var getInvoicesRequest = new Request.Builder()
                .url("https://service.citiz.fr/citiz/api/customer/%s/invoices?token=%s&providerId=%d".formatted(authInfo.customerId(), authInfo.token(), providerId))
                .get()
                .build();

        final var invoicesResponse = client.newCall(getInvoicesRequest)
                .execute()
                .body()
                .string();

        final var jsonInvoicesResponse = new JSONArray(invoicesResponse);

        return jsonInvoicesResponse.toList()
                .stream()
                .map(o -> (HashMap<String, Object>) o)
                .filter(json -> (Integer) json.get("invoiceNo") != -1)
                .map(json -> (Integer) json.get("invoiceId"))
                .filter(invoiceId -> invoiceId > invoiceOffset)
                .limit(batchSize)
                .toList();
    }

    private CitizAuthInfo authenticate(String username, String password, int providerId) throws AuthenticationException {
        final var authBody = new JSONObject()
                .put("username", username)
                .put("password", password)
                .put("providerID", providerId);

        final var authRequest = new Request.Builder()
                .url(AUTH_URI)
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Accept", "application/json, text/plain, */*")
                .post(RequestBody.create(authBody.toString(), MediaType.parse("application/json")))
                .build();

        final var call = client.newCall(authRequest);

        final JSONObject jsonBody;

        try {
            final Response response = call.execute();
            jsonBody = new JSONObject(response.body().string());
        } catch (IOException e) {
            throw new AuthenticationException(e);
        }

        final var userSettings = jsonBody
                .getJSONArray("results")
                .getJSONObject(0);

        final var token = userSettings.getString("token");
        final var customerId = userSettings.getInt("customerId");

        return new CitizAuthInfo(token, customerId);
    }

    @Override
    public void stop() {
    }

    private Map<String, String> offsetKey(String filename) {
        return Collections.singletonMap("FILENAME_FIELD", filename);
    }

    private Map<String, Integer> offsetValue(Integer pos) {
        return Collections.singletonMap("POSITION_FIELD", pos);
    }
}
