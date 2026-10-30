package com.example.myfirstapp.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real-time update detection service querying GitHub Releases API.
 * Automatically catches newer versions published on GitHub and provides direct APK / release URLs.
 */
public class UpdateChecker {
    public static final String CURRENT_VERSION = "v1.0.0";
    public static final String RELEASES_API_URL = "https://api.github.com/repos/Zeenz123/SmartSave/releases/latest";
    public static final String RELEASES_WEB_URL = "https://github.com/Zeenz123/SmartSave/releases/latest";

    public static class UpdateInfo {
        private final boolean updateAvailable;
        private final String latestVersion;
        private final String releaseNotes;
        private final String downloadUrl;
        private final String htmlUrl;

        public UpdateInfo(boolean updateAvailable, String latestVersion, String releaseNotes, String downloadUrl, String htmlUrl) {
            this.updateAvailable = updateAvailable;
            this.latestVersion = latestVersion;
            this.releaseNotes = releaseNotes;
            this.downloadUrl = downloadUrl;
            this.htmlUrl = htmlUrl;
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public String getReleaseNotes() {
            return releaseNotes;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }
    }

    /**
     * Checks GitHub for the latest published release.
     */
    public static UpdateInfo checkLatestRelease() {
        try {
            URI uri = URI.create(RELEASES_API_URL);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "SmartSave-Client");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            int code = conn.getResponseCode();
            if (code == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String json = sb.toString();
                String tagName = extractString(json, "\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
                String htmlUrl = extractString(json, "\"html_url\"\\s*:\\s*\"([^\"]+)\"");
                String body = extractString(json, "\"body\"\\s*:\\s*\"([^\"]+)\"");
                String downloadUrl = extractString(json, "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"");

                if (htmlUrl == null) htmlUrl = RELEASES_WEB_URL;
                if (downloadUrl == null) downloadUrl = htmlUrl;

                if (tagName != null && isNewer(tagName, CURRENT_VERSION)) {
                    return new UpdateInfo(true, tagName, body != null ? body.replace("\\n", "\n") : "New release with improvements and fixes.", downloadUrl, htmlUrl);
                } else {
                    return new UpdateInfo(false, tagName != null ? tagName : CURRENT_VERSION, "You are running the latest version.", downloadUrl, htmlUrl);
                }
            }
        } catch (Exception ignored) {
            // Offline, rate-limited, or repository still in initial release draft state
        }
        return new UpdateInfo(false, CURRENT_VERSION, "Current version up to date.", RELEASES_WEB_URL, RELEASES_WEB_URL);
    }

    public static boolean isNewer(String latest, String current) {
        if (latest == null || current == null) return false;
        String[] v1 = latest.replaceAll("[^0-9.]", "").split("\\.");
        String[] v2 = current.replaceAll("[^0-9.]", "").split("\\.");

        int len = Math.max(v1.length, v2.length);
        for (int i = 0; i < len; i++) {
            int num1 = i < v1.length && !v1[i].isEmpty() ? Integer.parseInt(v1[i]) : 0;
            int num2 = i < v2.length && !v2[i].isEmpty() ? Integer.parseInt(v2[i]) : 0;
            if (num1 > num2) return true;
            if (num1 < num2) return false;
        }
        return false;
    }

    private static String extractString(String json, String patternRegex) {
        Pattern pattern = Pattern.compile(patternRegex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
