package com.example.myfirstapp.service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Clearly identified Prototype Implementation of AI-assisted Medical Document Verification.
 * Evaluates document structural metadata, doctor registration format, recognized clinical terms,
 * and emergency diagnosis urgency scoring.
 */
public class MedicalVerificationService {
    private static final List<String> URGENT_DIAGNOSES = Arrays.asList(
            "acute", "emergency", "surgery", "fracture", "trauma", "icu", "cardiac",
            "hospitalization", "operation", "critical", "infection", "appendicitis"
    );

    private static final Pattern DOCTOR_REG_PATTERN = Pattern.compile("(?i)(reg|mci|nmc|lic)[-\\s]?\\d{4,8}");

    public static class VerificationResult {
        private final boolean approved;
        private final int confidenceScore;
        private final String diagnosticSummary;
        private final String failureReason;

        public VerificationResult(boolean approved, int confidenceScore, String diagnosticSummary, String failureReason) {
            this.approved = approved;
            this.confidenceScore = confidenceScore;
            this.diagnosticSummary = diagnosticSummary;
            this.failureReason = failureReason;
        }

        public boolean isApproved() {
            return approved;
        }

        public int getConfidenceScore() {
            return confidenceScore;
        }

        public String getDiagnosticSummary() {
            return diagnosticSummary;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    /**
     * AI-assisted verification algorithm checking document content against clinical emergency criteria.
     */
    public VerificationResult verifyDocument(String hospitalName, String doctorRegistration, String diagnosisText, String documentNotes) {
        if (hospitalName == null || hospitalName.trim().length() < 3) {
            return new VerificationResult(false, 15, null, "Invalid or missing Hospital / Medical Institution name");
        }

        if (doctorRegistration == null || !DOCTOR_REG_PATTERN.matcher(doctorRegistration.trim()).find()) {
            return new VerificationResult(false, 30, null, "Doctor Registration number missing or invalid format (Expected e.g. REG-49201 or MCI-10293)");
        }

        if (diagnosisText == null || diagnosisText.trim().length() < 5) {
            return new VerificationResult(false, 20, null, "Incomplete clinical diagnosis statement");
        }

        String combinedText = (diagnosisText + " " + (documentNotes != null ? documentNotes : "")).toLowerCase();

        int matches = 0;
        for (String keyword : URGENT_DIAGNOSES) {
            if (combinedText.contains(keyword)) {
                matches++;
            }
        }

        // Calculate AI confidence score
        int score = 40 + (matches * 15);
        if (score > 98) score = 98;

        if (matches == 0) {
            return new VerificationResult(false, score, null,
                    "Document analysis indicated a routine or non-emergency condition. Emergency withdrawal requires urgent clinical indication.");
        }

        String summary = String.format("AI Verified: Emergency medical report from %s (Reg: %s). Detected urgent markers: %d keyword matches. Confidence: %d%%.",
                hospitalName.trim(), doctorRegistration.trim(), matches, score);

        return new VerificationResult(true, score, summary, null);
    }
}
