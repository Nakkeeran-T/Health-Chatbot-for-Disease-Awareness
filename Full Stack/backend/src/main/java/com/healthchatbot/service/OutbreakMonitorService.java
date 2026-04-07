package com.healthchatbot.service;

import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.repository.OutbreakAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Outbreak Monitor Service
 *
 * Simulates integration with India's Integrated Disease Surveillance Programme (IDSP)
 * and Odisha's State Disease Monitoring Unit (SDMU).
 *
 * In production, this service would poll:
 * - IDSP API: https://idsp.mohfw.gov.in (weekly district-level reports)
 * - Odisha Health & Family Welfare Dept API
 * - WHO GOARN feeds for international outbreaks
 *
 * For this implementation it:
 * 1. Seeds real-world-like outbreak alert data for Odisha districts
 * 2. Provides district-level surveillance mock data for the scheduler
 * 3. Detects "spikes" by comparing current case counts vs thresholds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutbreakMonitorService {

    private final OutbreakAlertRepository alertRepository;

    // ─── Odisha district surveillance thresholds (cases/week to trigger alert) ─
    private static final int MALARIA_SPIKE_THRESHOLD = 20;
    private static final int DENGUE_SPIKE_THRESHOLD = 10;
    private static final int DIARRHOEA_SPIKE_THRESHOLD = 50;
    private static final int CHOLERA_SPIKE_THRESHOLD = 5;   // Very low — cholera is serious
    private static final int TYPHOID_SPIKE_THRESHOLD = 15;

    // ─── Odisha Districts (all 30 official districts) ────────────────────────
    public static final List<String> ODISHA_DISTRICTS = List.of(
        "Angul", "Balangir", "Balasore", "Bargarh", "Bhadrak",
        "Boudh", "Cuttack", "Debagarh", "Dhenkanal", "Gajapati",
        "Ganjam", "Jagatsinghpur", "Jajpur", "Jharsuguda", "Kalahandi",
        "Kandhamal", "Kendrapara", "Kendujhar", "Khordha", "Koraput",
        "Malkangiri", "Mayurbhanj", "Nabarangpur", "Nayagarh", "Nuapada",
        "Puri", "Rayagada", "Sambalpur", "Sonepur", "Sundargarh"
    );

    // ─── Seed Initial Outbreak Alerts ───────────────────────────────────────

    @Transactional
    public void seedInitialOutbreakData() {
        if (alertRepository.count() > 0) {
            log.info("✅ Outbreak alert database already seeded. Skipping.");
            return;
        }

        log.info("🚨 Seeding initial outbreak alert data for Odisha districts...");

        List<OutbreakAlert> alerts = new ArrayList<>();

        // Active High-severity alerts (representing current real-world disease burden)
        alerts.add(OutbreakAlert.builder()
            .title("Malaria Outbreak — Malkangiri")
            .titleHi("मलेरिया प्रकोप — मलकानगिरि")
            .titleOr("ମ୍ୟାଲେରିଆ ପ୍ରାଦୁର୍ଭାବ — ମାଲକାନଗିରି")
            .description("A significant spike in Plasmodium falciparum malaria cases has been reported in Malkangiri district. 47 confirmed cases in the past 7 days. High-risk areas: Kalimela, Mathili, Chitrakonda blocks. Health teams and ASHA workers have been alerted. Free diagnosis and treatment at all PHCs.")
            .descriptionHi("मलकानगिरि जिले में पिछले 7 दिनों में 47 मलेरिया के पक्के मामले सामने आए हैं। खतरा वाले इलाके: कालीमेला, माथिली और चित्रकोंडा ब्लॉक। सभी PHC में मुफ्त जाँच और इलाज उपलब्ध है।")
            .descriptionOr("ମାଲକାନଗିରି ଜିଲ୍ଲାରେ ଗତ 7 ଦିନ ଭିତରେ 47 ଟି ମ୍ୟାଲେରିଆ ଘଟଣା ଜଣାଯାଇଛି। ବିପଦ ଅଞ୍ଚଳ: କାଳୀମେଲା, ମଥିଲି, ଚିତ୍ରକୋଣ୍ଡା ବ୍ଲକ। ସମସ୍ତ PHCରେ ମାଗଣା ଚିକିତ୍ସା।")
            .disease("Malaria")
            .region("Odisha")
            .district("Malkangiri")
            .severity(OutbreakAlert.SeverityLevel.HIGH)
            .reportedCases(47)
            .active(true)
            .precautions("Sleep under insecticide-treated bed nets every night. Do not allow water to stagnate near homes. Report fever immediately to the nearest ASHA worker or PHC.")
            .contactNumber("104")
            .build());

        alerts.add(OutbreakAlert.builder()
            .title("Dengue Alert — Khordha & Cuttack")
            .titleHi("डेंगू अलर्ट — खोर्धा और कटक")
            .titleOr("ଡେଙ୍ଗୁ ଆଲର୍ଟ — ଖୋର୍ଦ୍ଧା ଓ କଟକ")
            .description("Rising dengue cases reported in urban areas of Khordha (Bhubaneswar) and Cuttack districts. 28 confirmed cases in the last 10 days. Aedes mosquito breeding sites identified in construction zones. Health teams conducting fogging operations.")
            .descriptionHi("भुवनेश्वर (खोर्धा) और कटक के शहरी इलाकों में पिछले 10 दिनों में 28 डेंगू के मामले सामने आए हैं। निर्माण क्षेत्रों में मच्छरों के प्रजनन स्थल मिले हैं।")
            .descriptionOr("ଭୁବନେଶ୍ୱର (ଖୋର୍ଦ୍ଧା) ଓ କଟକ ସହରରେ 10 ଦିନ ଭିତରେ 28 ଡେଙ୍ଗୁ ଘଟଣା। ଆଇଡ଼ ମଶା ବଂଶ ବୃଦ୍ଧି ଅଞ୍ଚଳ ଚିହ୍ନଟ।")
            .disease("Dengue")
            .region("Odisha")
            .district("Khordha")
            .severity(OutbreakAlert.SeverityLevel.MEDIUM)
            .reportedCases(28)
            .active(true)
            .precautions("Empty all water containers, flower vases, and coolers weekly. Use mosquito repellent during daytime. Seek medical care immediately if fever lasts more than 2 days.")
            .contactNumber("104")
            .build());

        alerts.add(OutbreakAlert.builder()
            .title("Waterborne Disease Alert — Ganjam Post-Flood")
            .titleHi("जलजनित बीमारी अलर्ट — गंजाम बाढ़ के बाद")
            .titleOr("ଜଳ ଜନିତ ରୋଗ ଆଲର୍ଟ — ଗଞ୍ଜାମ ବନ୍ୟା ପ୍ରଭାବ")
            .description("Following recent flooding, elevated risk of waterborne diseases (diarrhoea, cholera, typhoid) in Ganjam district. 62 acute diarrhoea cases reported in the past week. ORS packets being distributed by ASHA workers. Water purification tablets distributed in affected panchayats.")
            .descriptionHi("हालिया बाढ़ के बाद गंजाम जिले में जलजनित बीमारियों का खतरा बढ़ गया है। पिछले सप्ताह 62 दस्त के मामले सामने आए। ASHA कार्यकर्ता ORS बाँट रहे हैं।")
            .descriptionOr("ସ˙ˣ ବ˙ˣ˙ˣ ˣ ˣ ˣ˙ ˣ ˣ ˣˣ˙ ˣˣ ˣ˙ˣ˙ ˣ ˣˣˣ 62 ˣ˙˙ ˣˣ˙ˣ ˣˣ˙˙ˣˣˣ˙ˣˣ˙ˣ˙˙˙˙ˣˣˣ")
            .disease("Diarrhoea")
            .region("Odisha")
            .district("Ganjam")
            .severity(OutbreakAlert.SeverityLevel.HIGH)
            .reportedCases(62)
            .active(true)
            .precautions("Drink ONLY boiled or chlorinated water. Use ORS immediately for diarrhoea. Do not use floodwater for drinking or cooking. Maintain hand hygiene. Report to nearest PHC immediately.")
            .contactNumber("104")
            .build());

        alerts.add(OutbreakAlert.builder()
            .title("Japanese Encephalitis Alert — Koraput")
            .titleHi("जापानी दिमागी बुखार अलर्ट — कोरापुट")
            .titleOr("JE ଆଲର୍ଟ — କୋରାପୁଟ")
            .description("3 confirmed Japanese Encephalitis cases reported in Koraput district. All are children under 10 years. Parents urged to ensure JE vaccination for children 9-12 months under the Universal Immunization Programme. Fogging operations underway near affected villages.")
            .descriptionHi("कोरापुट जिले में 3 जापानी दिमागी बुखार के पक्के मामले सामने आए हैं। सभी 10 साल से कम उम्र के बच्चे हैं। UIP के तहत JE टीका लगवाएं।")
            .descriptionOr("କୋରାପୁଟ ଜିଲ୍ଲାରେ 3 ଟି JE ଘଟଣା। ସମସ୍ତ 10 ବର୍ଷ ତଳ ଶିଶୁ। ଶିଶୁ JE ଟୀକା ନିଶ୍ଚିତ କରନ୍ତୁ।")
            .disease("Japanese Encephalitis")
            .region("Odisha")
            .district("Koraput")
            .severity(OutbreakAlert.SeverityLevel.CRITICAL)
            .reportedCases(3)
            .active(true)
            .precautions("Ensure JE vaccination for all children 9-12 months. Seek emergency care IMMEDIATELY for high fever with seizures or confusion in children. Call 108 for ambulance.")
            .contactNumber("108")
            .build());

        alerts.add(OutbreakAlert.builder()
            .title("Malaria Cases Declining — Sundargarh")
            .titleHi("मलेरिया मामले घट रहे हैं — सुंदरगढ़")
            .titleOr("ମ˙ˣˣˣ ˣ˙ˣˣ ˣ˙ˣˣ˙ˣ — ˣˣˣˣˣ˙ˣˣˣ")
            .description("Malaria cases in Sundargarh district are declining following intensive IRS (Indoor Residual Spraying) campaign and LLIN (Long-Lasting Insecticidal Nets) distribution. Cases dropped from 45/week to 12/week over past month. Alert being monitored.")
            .descriptionHi("IRS और मच्छरदानी वितरण अभियान के बाद सुंदरगढ़ में मलेरिया के मामले घट रहे हैं। 45/सप्ताह से 12/सप्ताह हो गए।")
            .descriptionOr("IRS ˣ LLIN ˣˣˣ ˣˣˣ ˣˣˣˣ˙ˣ˙ˣˣ ˣˣˣˣ ˣˣ˙ˣˣˣ˙ˣˣ। 45 ˣˣˣˣ 12 ˣ˙˙ˣ।")
            .disease("Malaria")
            .region("Odisha")
            .district("Sundargarh")
            .severity(OutbreakAlert.SeverityLevel.LOW)
            .reportedCases(12)
            .active(true)
            .precautions("Continue sleeping under insecticide-treated bed nets. Report fever to nearest ASHA worker.")
            .contactNumber("104")
            .build());

        alertRepository.saveAll(alerts);
        log.info("✅ Seeded {} outbreak alerts for Odisha districts.", alerts.size());
    }

    // ─── Surveillance Data Simulation (mimics IDSP weekly reports) ─────────────

    /**
     * Simulates fetching weekly case counts from IDSP / State Surveillance Unit.
     * In production: replace with actual API calls to IDSP/Odisha SDMU endpoints.
     *
     * Returns a map of: diseaseName -> casesThisWeek (for the given district)
     */
    public Map<String, Integer> getSurveillanceData(String district) {
        // Simulate district-specific disease burden based on geography and season
        // based on actual Odisha epidemiological patterns
        Map<String, Integer> data = new HashMap<>();
        Random rng = new Random(district.hashCode() + LocalDateTime.now().getDayOfYear());

        // Tribal/coastal/flood-prone districts have higher baseline
        boolean isHighRiskDistrict = List.of(
            "Malkangiri", "Koraput", "Gajapati", "Rayagada", "Nabarangpur",
            "Kandhamal", "Kalahandi", "Nuapada", "Balangir"
        ).contains(district);

        boolean isCoastalDistrict = List.of(
            "Ganjam", "Puri", "Khordha", "Jagatsinghpur", "Kendrapara",
            "Bhadrak", "Balasore"
        ).contains(district);

        // Simulate monthly variation (monsoon = peaks in July-October)
        int month = LocalDateTime.now().getMonthValue();
        boolean isMonsoon = (month >= 6 && month <= 10);
        double seasonalMultiplier = isMonsoon ? 2.5 : 1.0;

        // Malaria cases — highest in tribal/forest districts
        int malariaBase = isHighRiskDistrict ? 18 : 5;
        data.put("Malaria", (int) ((malariaBase + rng.nextInt(15)) * seasonalMultiplier));

        // Dengue — higher in urban/coastal districts
        int dengueBase = isCoastalDistrict ? 8 : 3;
        data.put("Dengue", (int) ((dengueBase + rng.nextInt(10)) * (isMonsoon ? 2.0 : 1.0)));

        // Diarrhoea — higher post-flood, monsoon
        int diarrhoeaBase = isCoastalDistrict ? 25 : 12;
        data.put("Diarrhoea", (int) ((diarrhoeaBase + rng.nextInt(30)) * seasonalMultiplier));

        // Cholera — very low, but can spike post-flood
        int choleraBase = (isCoastalDistrict && isMonsoon) ? 3 : 0;
        data.put("Cholera", choleraBase + rng.nextInt(4));

        // Typhoid — moderate everywhere
        data.put("Typhoid", 5 + rng.nextInt(12));

        log.debug("Surveillance data for {} (month={}, highRisk={}, coastal={}): {}",
            district, month, isHighRiskDistrict, isCoastalDistrict, data);

        return data;
    }

    /**
     * Checks if a disease case count exceeds the alert threshold for its category.
     */
    public boolean exceedsThreshold(String disease, int cases) {
        return switch (disease) {
            case "Malaria"    -> cases >= MALARIA_SPIKE_THRESHOLD;
            case "Dengue"     -> cases >= DENGUE_SPIKE_THRESHOLD;
            case "Diarrhoea"  -> cases >= DIARRHOEA_SPIKE_THRESHOLD;
            case "Cholera"    -> cases >= CHOLERA_SPIKE_THRESHOLD;
            case "Typhoid"    -> cases >= TYPHOID_SPIKE_THRESHOLD;
            default           -> false;
        };
    }

    /**
     * Determines severity level based on case count and disease type.
     */
    public OutbreakAlert.SeverityLevel determineSeverity(String disease, int cases) {
        return switch (disease) {
            case "Cholera" -> cases >= 10 ? OutbreakAlert.SeverityLevel.CRITICAL
                           : cases >= 5  ? OutbreakAlert.SeverityLevel.HIGH
                           :               OutbreakAlert.SeverityLevel.MEDIUM;
            case "Malaria" -> cases >= 50 ? OutbreakAlert.SeverityLevel.CRITICAL
                           : cases >= 30  ? OutbreakAlert.SeverityLevel.HIGH
                           :               OutbreakAlert.SeverityLevel.MEDIUM;
            case "Dengue"  -> cases >= 30 ? OutbreakAlert.SeverityLevel.CRITICAL
                           : cases >= 15  ? OutbreakAlert.SeverityLevel.HIGH
                           :               OutbreakAlert.SeverityLevel.MEDIUM;
            default        -> cases >= 100 ? OutbreakAlert.SeverityLevel.HIGH
                           :                OutbreakAlert.SeverityLevel.MEDIUM;
        };
    }

    /**
     * Returns whether an active alert already exists for this district + disease.
     */
    public boolean hasActiveAlert(String district, String disease) {
        return !alertRepository.findByDistrictIgnoreCaseAndActiveTrue(district).stream()
            .filter(a -> a.getDisease().equalsIgnoreCase(disease))
            .toList().isEmpty();
    }

    /**
     * Creates and saves a new outbreak alert for the given spike.
     */
    @Transactional
    public OutbreakAlert createSpikeAlert(String district, String disease, int cases,
                                          OutbreakAlert.SeverityLevel severity) {
        String precautions = getPrecautionsForDisease(disease);
        String contactNumber = "104";

        OutbreakAlert alert = OutbreakAlert.builder()
            .title(disease + " Spike Detected — " + district)
            .titleHi(getHindiDiseaseName(disease) + " वृद्धि — " + district)
            .titleOr(getOdiaDiseaseName(disease) + " ବୃଦ୍ଧି — " + district)
            .description(String.format(
                "Automated surveillance detected %d new %s cases in %s district this week, exceeding the alert threshold. " +
                "Immediate public health response initiated. Please take precautions and report suspicious symptoms to the nearest PHC.",
                cases, disease, district))
            .descriptionHi(String.format(
                "इस सप्ताह %s जिले में %d नए %s मामले सामने आए हैं, जो अलर्ट सीमा से अधिक है। " +
                "कृपया सावधानी बरतें और किसी भी लक्षण को तुरंत PHC में रिपोर्ट करें।",
                district, cases, getHindiDiseaseName(disease)))
            .descriptionOr(String.format(
                "ଏହି ସପ୍ତାହ %s ଜିଲ୍ଲାରେ %d ଟି ନୂଆ %s ଘଟଣା ଜଣା ପଡ଼ିଛି। " +
                "ଦୟାକରି ସତର୍କ ରୁହନ୍ତୁ ଓ ଲକ୍ଷଣ ଦେଖିଲେ PHCକୁ ଯାଆନ୍ତୁ।",
                district, cases, getOdiaDiseaseName(disease)))
            .disease(disease)
            .region("Odisha")
            .district(district)
            .severity(severity)
            .reportedCases(cases)
            .active(true)
            .precautions(precautions)
            .contactNumber(contactNumber)
            .build();

        return alertRepository.save(alert);
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private String getPrecautionsForDisease(String disease) {
        return switch (disease) {
            case "Malaria"   -> "Sleep under insecticide-treated bed nets every night. Eliminate stagnant water. Report fever immediately to nearest PHC.";
            case "Dengue"    -> "Empty all water containers weekly. Use mosquito repellent during day. Seek care for fever lasting more than 2 days.";
            case "Diarrhoea" -> "Drink only boiled water. Use ORS solution. Wash hands with soap. Visit PHC if symptoms persist more than 1 day.";
            case "Cholera"   -> "EMERGENCY: Use ORS immediately. Go to hospital NOW. Drink only boiled water. Do not use flood/river water.";
            case "Typhoid"   -> "Drink only boiled water. Avoid street food. Get Typhoid vaccine from PHC. Complete antibiotic course as prescribed.";
            default          -> "Follow standard hygiene precautions. Consult nearest PHC for symptoms.";
        };
    }

    private String getHindiDiseaseName(String disease) {
        return switch (disease) {
            case "Malaria"    -> "मलेरिया";
            case "Dengue"     -> "डेंगू";
            case "Diarrhoea"  -> "दस्त";
            case "Cholera"    -> "हैजा";
            case "Typhoid"    -> "टाइफाइड";
            default           -> disease;
        };
    }

    private String getOdiaDiseaseName(String disease) {
        return switch (disease) {
            case "Malaria"    -> "ମ˙ˣˣˣˣˣ";
            case "Dengue"     -> "ˣ˙ˣˣˣ";
            case "Diarrhoea"  -> "ˣˣˣ˙ˣ";
            case "Cholera"    -> "ˣ˙ˣˣˣˣ";
            case "Typhoid"    -> "ˣˣˣˣˣ˙ˣ";
            default           -> disease;
        };
    }
}
