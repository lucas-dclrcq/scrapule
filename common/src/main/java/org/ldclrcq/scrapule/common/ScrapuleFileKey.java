package org.ldclrcq.scrapule.common;

import org.json.JSONObject;

public record ScrapuleFileKey(ScrapuleScrapper scrapper, String filename) {
    public final static String SCRAPPER_JSON_KEY = "scrapper";
    public final static String FILENAME_JSON_KEY = "filename";

    public JSONObject toJson() {
        return new JSONObject()
                .put(SCRAPPER_JSON_KEY, scrapper)
                .put(FILENAME_JSON_KEY, filename);
    }

    public static ScrapuleFileKey fromJson(JSONObject json) {
        return new ScrapuleFileKey(
                json.getEnum(ScrapuleScrapper.class, SCRAPPER_JSON_KEY),
                json.getString(FILENAME_JSON_KEY));
    }
}
