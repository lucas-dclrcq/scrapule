package org.ldclrcq.document.scrapper.scaleway;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class CitizSourceConnectorTest {
    private static OkHttpClient client;

    @BeforeAll
    static void init() throws Exception {
        client = new OkHttpClient();
    }

    @Test
    void name() throws IOException {
        final var uri = "https://service.citiz.fr/citiz/api/customer/login";

        final var body = String.format("""
                {"username":"%s","password":"%s","providerId":16}
                """);

        final var request = new Request.Builder()
                .url(uri)
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Accept", "application/json, text/plain, */*")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        final var call = client.newCall(request);
        final var response = call.execute();

        final var jsonBody = new JSONObject(response.body().string());
        final var userSettings = jsonBody
                .getJSONArray("results")
                .getJSONObject(0);

        final var token = userSettings.getString("token");
        final var customerId = userSettings.getNumber("customerId");

        final var getInvoicesRequest = new Request.Builder()
                .url("https://service.citiz.fr/citiz/api/customer/%s/invoices?token=%s&providerId=16".formatted(customerId, token))
                .get()
                .build();

        final var invoicesResponse = client.newCall(getInvoicesRequest)
                .execute()
                .body()
                .string();

        final var jsonInvoicesResponse = new JSONArray(invoicesResponse);

        final var offset = 1;

        record InvoiceNoAndId(int no, int id) { }

        final var invoiceIds = jsonInvoicesResponse.toList()
                .stream()
                .map(o -> (HashMap<String, Object>) o)
                .map(jsonObject -> {

                    final var invoiceId = (Integer) jsonObject.get("invoiceId");
                    final var invoiceNo = (Integer) jsonObject.get("invoiceNo");

                    return new InvoiceNoAndId(invoiceNo, invoiceId);
                })
                .filter(invoiceNoAndId -> invoiceNoAndId.no() != -1)
                .filter(invoiceId -> invoiceId.id >   offset)
                .map(InvoiceNoAndId::id)
                .toList();


        for (var invoiceId : invoiceIds) {
            System.out.println(invoiceId);
            final var s = "https://service.citiz.fr/citiz/api/customer/97601/invoices/%d?token=%s&providerId=16".formatted(invoiceId, token);


            final var downloadRequest = new Request.Builder()
                    .url(s)
                    .get()
                    .build();

            final var downloadCall = client.newCall(downloadRequest);
            final var downloadResponse = downloadCall.execute();
            final var inputStream = downloadResponse.body().byteStream();

            final var test = File.createTempFile("citiz_invoice_" + invoiceId +"_", ".pdf");

            FileUtils.copyInputStreamToFile(inputStream, test);
        }

        assertThat(response.code()).isEqualTo(200);
    }
}