package com.healthchatbot.service;

import com.healthchatbot.entity.DiseaseInfo;
import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.repository.DiseaseInfoRepository;
import com.healthchatbot.repository.OutbreakAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NLPService {

    private final DiseaseInfoRepository diseaseInfoRepository;
    private final OutbreakAlertRepository outbreakAlertRepository;

    // Intent keywords map: intent -> keywords (EN, HI, OR)
    private static final Map<String, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        INTENT_KEYWORDS.put("greeting", Arrays.asList(
                "hello", "hi", "hey", "namaste", "namaskar", "help", "start",
                "नमस्ते", "हैलो", "ହେଲୋ", "ନମସ୍କାର"));
        INTENT_KEYWORDS.put("disease_symptoms", Arrays.asList(
                "symptom", "symptoms", "sign", "signs", "feel", "feeling", "sick", "ill", "unwell",
                "pain", "ache", "fever", "cough", "cold", "diarrhea", "vomiting",
                "लक्षण", "बीमारी", "बुखार", "खांसी", "दर्द",
                "ଲକ୍ଷଣ", "ଜ୍ୱର", "ଖାଂସି", "ଯନ୍ତ୍ରଣା"));
        INTENT_KEYWORDS.put("disease_prevention", Arrays.asList(
                "prevent", "prevention", "avoid", "protection", "precaution", "safe", "safety",
                "रोकथाम", "बचाव", "सुरक्षा",
                "ପ୍ରତିରୋଧ", "ସୁରକ୍ଷା", "ରକ୍ଷା"));
        INTENT_KEYWORDS.put("vaccine", Arrays.asList(
                "vaccine", "vaccination", "immunization", "shot", "dose", "jab", "inject",
                "टीका", "वैक्सीन", "टीकाकरण",
                "ଟୀକା", "ଭ୍ୟାକ୍ସିନ", "ଟୀକାକରଣ"));
        INTENT_KEYWORDS.put("outbreak_alert", Arrays.asList(
                "outbreak", "alert", "epidemic", "spread", "cases", "emergency", "news", "latest",
                "प्रकोप", "अलर्ट", "महामारी",
                "ପ୍ରାଦୁର୍ଭାବ", "ଆଲର୍ଟ", "ମହାମାରୀ"));
        INTENT_KEYWORDS.put("malaria", Arrays.asList(
                "malaria", "mosquito", "plasmodium",
                "मलेरिया", "मच्छर",
                "ମ୍ୟାଲେରିଆ", "ମଶା"));
        INTENT_KEYWORDS.put("dengue", Arrays.asList(
                "dengue", "dengue fever", "aedes",
                "डेंगू", "डेंगी",
                "ଡେଙ୍ଗୁ"));
        INTENT_KEYWORDS.put("tuberculosis", Arrays.asList(
                "tuberculosis", "tb", "tubercle", "lung",
                "तपेदिक", "टीबी", "क्षय",
                "ଯକ୍ଷ୍ମା", "ଟିବି"));
        INTENT_KEYWORDS.put("cholera", Arrays.asList(
                "cholera", "water", "diarrhea",
                "हैजा", "कॉलरा",
                "କଲେରା", "ଝାଡ଼ା"));
        INTENT_KEYWORDS.put("covid", Arrays.asList(
                "covid", "corona", "coronavirus", "covid19", "covid-19",
                "कोविड", "कोरोना",
                "କୋଭିଡ", "କୋରୋନା"));
        INTENT_KEYWORDS.put("typhoid", Arrays.asList(
                "typhoid", "enteric fever",
                "टाइफाइड",
                "ଟାଇଫଏଡ"));
        INTENT_KEYWORDS.put("child_health", Arrays.asList(
                "child", "baby", "infant", "newborn", "toddler", "kid",
                "बच्चा", "शिशु",
                "ଶିଶୁ", "ପିଲା"));
        INTENT_KEYWORDS.put("emergency", Arrays.asList(
                "emergency", "ambulance", "urgent", "critical", "hospital", "doctor",
                "आपातकाल", "अस्पताल", "डॉक्टर",
                "ଜରୁରୀ", "ଡାକ୍ତର", "ଅସ୍ପତାଳ"));
        INTENT_KEYWORDS.put("bye", Arrays.asList(
                "bye", "goodbye", "thank", "thanks", "ok", "done", "exit",
                "धन्यवाद", "अलविदा",
                "ଧନ୍ୟବାଦ", "ବିଦାୟ"));
    }

    public String detectIntent(String message) {
        String lower = message.toLowerCase().trim();
        String bestIntent = "general_query";
        int maxMatches = 0;

        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int matches = 0;
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword.toLowerCase())) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestIntent = entry.getKey();
            }
        }
        return bestIntent;
    }

    public double calculateConfidence(String message, String intent) {
        if ("general_query".equals(intent))
            return 0.4;
        String lower = message.toLowerCase();
        List<String> keywords = INTENT_KEYWORDS.getOrDefault(intent, Collections.emptyList());
        long matched = keywords.stream().filter(k -> lower.contains(k.toLowerCase())).count();
        return Math.min(0.95, 0.6 + (matched * 0.1));
    }

    /**
     * Attempts to find a disease in the database matching the user's message.
     * If found, returns a rich formatted response built from DB data.
     * Returns null if no disease match is found, allowing callers to fall back.
     *
     * This is the primary entry point for DB-driven responses — it is called
     * BEFORE any ML API or static fallback to ensure real DB data is always preferred.
     */
    public String resolveFromDatabase(String message, String intent, String language) {
        DiseaseInfo matched = findBestDiseaseMatch(message);
        if (matched == null) return null;
        log.info("DB match found: '{}' for message: '{}'", matched.getName(), message);
        return buildDiseaseResponse(matched, intent, language);
    }

    public String generateResponse(String message, String intent, String language) {
        // Handle explicit intents that shouldn't trigger a broad disease DB keyword search
        switch (intent) {
            case "greeting": return getGreetingResponse(language);
            case "vaccine": return getVaccineResponse(language);
            case "outbreak_alert": return getAlertResponse(language);
            case "child_health": return getChildHealthResponse(language);
            case "emergency": return getEmergencyResponse(language);
            case "bye": return getByeResponse(language);
        }

        // Reuse DB resolution so disease names always trigger rich cards
        String dbResponse = resolveFromDatabase(message, intent, language);
        if (dbResponse != null) return dbResponse;

        // Fallback to intent-based patterns for remaining intents
        return switch (intent) {
            case "greeting"          -> getGreetingResponse(language);
            case "disease_symptoms"  -> getDiseaseSymptomResponse(language);
            case "disease_prevention"-> getPreventionResponse(language);
            case "vaccine"           -> getVaccineResponse(language);
            case "outbreak_alert"    -> getAlertResponse(language);
            case "malaria"           -> getMalariaResponse(language);
            case "dengue"            -> getDengueResponse(language);
            case "tuberculosis"      -> getTBResponse(language);
            case "cholera"           -> getCholeraResponse(language);
            case "covid"             -> getCovidResponse(language);
            case "typhoid"           -> getTyphoidResponse(language);
            case "child_health"      -> getChildHealthResponse(language);
            case "emergency"         -> getEmergencyResponse(language);
            case "bye"               -> getByeResponse(language);
            default                  -> getDefaultResponse(language);
        };
    }

    /**
     * Tries every meaningful word in the message against the DB.
     * Returns the best (name-exact) match first, then any partial match.
     * Skips common stop/filler words so "tell me about malaria" hits "malaria", not "tell".
     */
    private static final Set<String> STOP_WORDS = Set.of(
        "tell", "me", "about", "what", "is", "are", "the", "a", "an",
        "how", "to", "for", "of", "in", "on", "do", "i", "my", "can",
        "you", "and", "or", "it", "its", "this", "that", "which", "with",
        "give", "show", "explain", "information", "info", "details",
        "have", "has", "had", "was", "were", "am", "be", "been", "being",
        "please", "help", "know", "want", "need", "get"
    );

    private DiseaseInfo findBestDiseaseMatch(String message) {
        String lower = message.toLowerCase().trim();
        String[] words = lower.split("[\\s,;.!?]+");

        // Priority 1: exact name match
        for (String word : words) {
            if (word.length() < 3 || STOP_WORDS.contains(word)) continue;
            Optional<DiseaseInfo> exact = diseaseInfoRepository.findByNameIgnoreCase(word);
            if (exact.isPresent()) return exact.get();
        }

        // Priority 2: try 2-word phrases (e.g. "whooping cough", "dengue fever")
        for (int i = 0; i < words.length - 1; i++) {
            if (STOP_WORDS.contains(words[i])) continue;
            String phrase = words[i] + " " + words[i + 1];
            List<DiseaseInfo> results = diseaseInfoRepository.searchByKeyword(phrase);
            if (!results.isEmpty()) return results.get(0);
        }

        // Priority 3: try each significant single word
        for (String word : words) {
            if (word.length() < 3 || STOP_WORDS.contains(word)) continue;
            List<DiseaseInfo> results = diseaseInfoRepository.searchByKeyword(word);
            if (!results.isEmpty()) return results.get(0);
        }

        return null;
    }

    private boolean isSpecificDisease(String intent) {
        return Arrays.asList("malaria", "dengue", "tuberculosis", "cholera", "covid", "typhoid").contains(intent);
    }

    private String buildDiseaseResponse(DiseaseInfo d, String intent, String lang) {
        // Resolve localised fields
        String name       = "hi".equals(lang) && d.getNameHi()       != null ? d.getNameHi()
                          : "or".equals(lang) && d.getNameOr()       != null ? d.getNameOr()       : d.getName();
        String symptoms   = "hi".equals(lang) && d.getSymptomsHi()   != null ? d.getSymptomsHi()
                          : "or".equals(lang) && d.getSymptomsOr()   != null ? d.getSymptomsOr()   : d.getSymptoms();
        String prevention = "hi".equals(lang) && d.getPreventionHi() != null ? d.getPreventionHi()
                          : "or".equals(lang) && d.getPreventionOr() != null ? d.getPreventionOr() : d.getPrevention();

        // Prevention-only request
        if ("disease_prevention".equals(intent)) {
            return "🛡️ **Prevention — " + name + "**\n\n" + prevention;
        }

        // Full rich card using all DB fields
        StringBuilder sb = new StringBuilder();
        sb.append("🦠 **").append(name).append("**");

        // Badges
        if (d.getCategory() != null && !d.getCategory().isBlank()) {
            sb.append(" · _").append(d.getCategory()).append("_");
        }
        if (d.getIcdCode() != null && !d.getIcdCode().isBlank()) {
            sb.append(" · ICD: `").append(d.getIcdCode()).append("`");
        }
        sb.append("\n\n");

        // Description
        if (d.getDescription() != null && !d.getDescription().isBlank()) {
            sb.append(d.getDescription()).append("\n\n");
        }

        // Contagious / Age group info
        if (Boolean.TRUE.equals(d.getContagious())) {
            sb.append("⚠️ *This disease is contagious.*\n\n");
        }
        if (d.getAffectedAgeGroup() != null && !d.getAffectedAgeGroup().isBlank()) {
            sb.append("👥 **Affected group:** ").append(d.getAffectedAgeGroup()).append("\n\n");
        }

        // Symptoms
        if (symptoms != null && !symptoms.isBlank()) {
            sb.append("🤒 **Symptoms:**\n").append(symptoms).append("\n\n");
        }

        // Prevention
        if (prevention != null && !prevention.isBlank()) {
            sb.append("🛡️ **Prevention:**\n").append(prevention).append("\n\n");
        }

        // Treatment
        if (d.getTreatment() != null && !d.getTreatment().isBlank()) {
            sb.append("💊 **Treatment:**\n").append(d.getTreatment()).append("\n\n");
        }

        sb.append("⚠️ *This is AI-generated health information. Please consult a qualified doctor for diagnosis and treatment.*");

        return sb.toString();
    }

    // ─── Time-aware greeting helper ──────────────────────────────────────────
    private String timeGreeting(String lang) {
        int hour = LocalTime.now().getHour();
        return switch (lang) {
            case "hi" -> hour < 12 ? "सुप्रभात!" : (hour < 17 ? "नमस्ते!" : "शुभ संध्या!");
            case "or" -> hour < 12 ? "ଶୁଭ ସକାଳ!" : (hour < 17 ? "ନମସ୍କାର!" : "ଶୁଭ ସନ୍ଧ୍ୟା!");
            default   -> hour < 12 ? "Good morning!" : (hour < 17 ? "Hello!" : "Good evening!");
        };
    }

    private static final Random RNG = new Random();
    private String pick(List<String> variants) {
        return variants.get(RNG.nextInt(variants.size()));
    }

    // --- Response builders by language (with variants) ---

    private String getGreetingResponse(String lang) {
        String g = timeGreeting(lang);
        return switch (lang) {
            case "hi" -> pick(List.of(
                "🙏 **" + g + " मैं आपका AI स्वास्थ्य सहायक हूं।**\n\n" +
                "मैं इन विषयों में मदद कर सकता हूं:\n" +
                "• 🦠 रोग के लक्षण और जानकारी\n• 💉 टीकाकरण कार्यक्रम\n" +
                "• 🚨 प्रकोप अलर्ट\n• 🛡️ रोकथाम के उपाय\n\n" +
                "आप क्या जानना चाहते हैं?",

                "👋 **" + g + " आपके स्वास्थ्य सहायक में स्वागत है!**\n\n" +
                "मुझसे पूछें:\n" +
                "• 🤒 बीमारियों के लक्षण\n• 💉 टीकाकरण की जानकारी\n" +
                "• 🚨 स्वास्थ्य अलर्ट\n• 🛡️ बचाव के उपाय\n\n" +
                "बताइए, मैं कैसे मदद करूं?"
            ));
            case "or" -> pick(List.of(
                "🙏 **" + g + " ମୁଁ ଆପଣଙ୍କ AI ସ୍ୱାସ୍ଥ୍ୟ ସହାୟକ।**\n\n" +
                "ମୁଁ ଆପଣଙ୍କୁ ଏଥିରେ ସାହାଯ୍ୟ କରିପାରିବି:\n" +
                "• 🦠 ରୋଗ ଲକ୍ଷଣ ଓ ତଥ୍ୟ\n• 💉 ଟୀକାକରଣ ସୂଚୀ\n" +
                "• 🚨 ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ\n• 🛡️ ପ୍ରତିରୋଧ ଉପାୟ\n\n" +
                "ଆପଣ କଣ ଜାଣିବାକୁ ଚାହୁଁଛନ୍ତି?",

                "👋 **" + g + " ଆପଣଙ୍କ ସ୍ୱାସ୍ଥ୍ୟ ଚ୍ୟାଟବଟ୍‌ରେ ସ୍ୱାଗତ!**\n\n" +
                "ମୁଁ ଆପଣଙ୍କୁ ରୋଗ, ଟୀକା, ଆଲର୍ଟ ଓ ପ୍ରତିରୋଧ ବିଷୟରେ ସାହାଯ୍ୟ କରିପାରିବି।\n\n" +
                "ଆଜି ଆପଣ କଣ ଜାଣିବାକୁ ଚାହୁଁଛନ୍ତି? 😊"
            ));
            default -> pick(List.of(
                "🙏 **" + g + " I'm your AI Health Assistant.**\n\n" +
                "I can help you with:\n" +
                "• 🦠 Disease symptoms & information\n• 💉 Vaccination schedules\n" +
                "• 🚨 Outbreak alerts\n• 🛡️ Prevention measures\n\n" +
                "What would you like to know about today?",

                "👋 **" + g + " Welcome to your personal Health Chatbot!**\n\n" +
                "I'm here to guide you on:\n" +
                "• 🤒 Understanding disease symptoms\n• 💉 Vaccine & immunization info\n" +
                "• 🚨 Outbreak alerts near you\n• 🛡️ Prevention & hygiene tips\n\n" +
                "Feel free to ask — what's on your mind?",

                "🌟 **" + g + " I'm your AI-powered Health Guide.**\n\n" +
                "Ask me anything about diseases, vaccines, alerts or prevention.\n\n" +
                "How can I assist you today? 😊"
            ));
        };
    }

    private String getDiseaseSymptomResponse(String lang) {
        return switch (lang) {
            case "hi" -> pick(List.of(
                "🦠 **अपने लक्षण बताएं, मैं संभावित बीमारी पहचानने में मदद करूंगा।**\n\n" +
                "जैसे: *'मुझे बुखार और सिरदर्द है'*\n\nया सीधे पूछें: मलेरिया, डेंगू, टीबी, हैजा, टाइफाइड",
                "🤒 **आप क्या महसूस कर रहे हैं?**\n\nअपने लक्षण बताएं — जितने लक्षण, उतना सटीक अनुमान!\n\nउदाहरण: बुखार, खांसी, सिरदर्द, दस्त"
            ));
            case "or" -> pick(List.of(
                "🦠 **ଆପଣଙ୍କ ଲକ୍ଷଣ ଦିଅନ୍ତୁ, ମୁଁ ରୋଗ ଚିହ୍ନଟ କରିବାରେ ସାହାଯ୍ୟ କରିବି।**\n\n" +
                "ଯଥା: *'ମୋର ଜ୍ୱର ଓ ମୁଣ୍ଡ ବ୍ୟଥା ଅଛି'*\n\nବା ସିଧା ପ୍ରଶ୍ନ: ମ୍ୟାଲେରିଆ, ଡେଙ୍ଗୁ, ଯକ୍ଷ୍ମା",
                "🤒 **ଆପଣ କଣ ଅନୁଭବ କରୁଛନ୍ତି?**\n\nଲକ୍ଷଣ ବର୍ଣ୍ଣନା କରନ୍ତୁ — ଅଧିକ ଲକ୍ଷଣ, ଅଧିକ ସଠିକ ଅନୁମାନ!"
            ));
            default -> pick(List.of(
                "🦠 **Tell me your symptoms and I'll help identify possible conditions.**\n\n" +
                "You can describe them naturally, like: *'I have fever and chills'*\n\n" +
                "Or ask directly: **Malaria, Dengue, TB, Cholera, Typhoid, COVID-19**",
                "🤒 **What symptoms are you experiencing?**\n\n" +
                "Describe how you're feeling — the more symptoms you share, the more accurate my prediction! 💡\n\n" +
                "**Examples:** fever, cough, headache, rash, vomiting, diarrhea",
                "🩺 **I can help identify diseases based on your symptoms.**\n\n" +
                "Just type what you're feeling — I understand natural language!\n\n" +
                "*e.g., 'I have high fever with chills and headache'*"
            ));
        };
    }

    private String getPreventionResponse(String lang) {
        return switch (lang) {
            case "hi" -> pick(List.of(
                "🛡️ **रोग रोकथाम के उपाय**\n\n" +
                "1. 🧼 नियमित रूप से हाथ धोएं\n2. 💧 शुद्ध पानी पिएं\n" +
                "3. 🦟 मच्छरदानी का उपयोग करें\n4. 💉 समय पर टीकाकरण कराएं\n" +
                "5. 🥗 पोषण युक्त भोजन खाएं\n6. 🏥 नियमित स्वास्थ्य जांच कराएं",
                "🩺 **स्वस्थ रहने के बेहतरीन तरीके**\n\n" +
                "✅ खाने से पहले और शौच के बाद हाथ धोएं\n" +
                "✅ मच्छरों वाले इलाकों में पूरे कपड़े पहनें\n" +
                "✅ बच्चों को नियमित टीका लगवाएं\n" +
                "✅ बीमारी के शुरुआती लक्षण आने पर डॉक्टर से मिलें"
            ));
            case "or" -> pick(List.of(
                "🛡️ **ସାଧାରଣ ରୋଗ ପ୍ରତିରୋଧ ଉପାୟ**\n\n" +
                "1. 🧼 ନିୟମିତ ହାତ ଧୁଅନ୍ତୁ\n2. 💧 ବିଶୁଦ୍ଧ ଜଳ ପିଅନ୍ତୁ\n" +
                "3. 🦟 ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ\n4. 💉 ସମୟ ଅନୁଯାୟୀ ଟୀକା ନିଅନ୍ତୁ\n" +
                "5. 🥗 ପୌଷ୍ଟିକ ଖାଦ୍ୟ ଖାଆନ୍ତୁ",
                "🩺 **ସୁସ୍ଥ ରହିବାର ଉଣ୍ଡ ଉପାୟ**\n\n" +
                "✅ ଖାଇବା ପୂର୍ବରୁ ଓ ଶୌଚ ପରେ ହାତ ଧୁଅନ୍ତୁ\n" +
                "✅ ଶିଶୁଙ୍କୁ ନିୟମିତ ଟୀକା ଦିଅନ୍ତୁ\n" +
                "✅ ଲକ୍ଷଣ ଦେଖିଲেই ଡାକ୍ତରଙ୍କ ପାଖکୁ ଯାଆନ୍ତୁ"
            ));
            default -> pick(List.of(
                "🛡️ **General Disease Prevention Tips**\n\n" +
                "1. 🧼 Wash hands frequently with soap\n2. 💧 Drink purified/boiled water\n" +
                "3. 🦟 Use mosquito nets & repellents\n4. 💉 Get vaccinated on time\n" +
                "5. 🥗 Eat nutritious food\n6. 🏥 Regular health check-ups\n" +
                "7. 🚮 Maintain clean surroundings",
                "🩺 **Staying Healthy: Key Prevention Practices**\n\n" +
                "✅ Wash hands before eating and after the toilet\n" +
                "✅ Wear full-sleeve clothes in mosquito-prone areas\n" +
                "✅ Get your children vaccinated on schedule\n" +
                "✅ See a doctor early — don't wait for symptoms to worsen\n\n" +
                "💡 *Which disease prevention would you like to know more about?*"
            ));
        };
    }

    private String getVaccineResponse(String lang) {
        return switch (lang) {
            case "hi" -> "💉 **टीकाकरण कार्यक्रम**\n\n" +
                    "राष्ट्रीय स्वास्थ्य मिशन के अंतर्गत मुफ्त टीके उपलब्ध हैं:\n\n" +
                    "👶 **शिशुओं के लिए:** BCG, पोलियो, DPT, हेपेटाइटिस-B\n" +
                    "🧒 **बच्चों के लिए:** MMR, खसरा, विटामिन A\n" +
                    "👩 **गर्भवती महिलाओं के लिए:** Td टॉक्साइड\n\n" +
                    "नजदीकी आंगनवाड़ी या स्वास्थ्य केंद्र जाएं।";
            case "or" -> "💉 **ଟୀକାକରଣ ସୂଚୀ**\n\n" +
                    "ଜାତୀୟ ସ୍ୱାସ୍ଥ୍ୟ ମିଶନ ଅଧୀନରେ ମାଗଣା ଟୀକା:\n\n" +
                    "👶 **ଶିଶୁ:** BCG, ପୋଲିଓ, DPT, ହେପାଟାଇଟିସ-B\n" +
                    "🧒 **ପିଲା:** MMR, ହାମ, ଭିଟାମିନ A\n" +
                    "👩 **ଗର୍ଭବତୀ:** Td ଟଏଡ\n\n" +
                    "ନିକଟ ଆଙ୍ଗନବାଡ଼ି ବା ସ୍ୱାସ୍ଥ୍ୟ କେନ୍ଦ୍ରରେ ଯୋଗାଯୋଗ କରନ୍ତୁ।";
            default -> "💉 **Vaccination Schedule (NHM India)**\n\n" +
                    "Free vaccines available at government health centres:\n\n" +
                    "👶 **Newborns:** BCG, OPV, Hepatitis-B\n" +
                    "🍼 **6 weeks:** DPT, Polio, Hib, Rotavirus\n" +
                    "🧒 **9-12 months:** Measles (MR), Vitamin A\n" +
                    "👩 **Pregnant women:** Td Toxoid (2 doses)\n\n" +
                    "Visit your nearest Anganwadi or Primary Health Centre (PHC).";
        };
    }

    private String getAlertResponse(String lang) {
        List<OutbreakAlert> alerts = outbreakAlertRepository.findByActiveTrue();
        if (alerts.isEmpty()) {
            return switch (lang) {
                case "hi" -> "✅ **वर्तमान में कोई सक्रिय प्रकोप अलर्ट नहीं है।**\n\nसावधानी बरतते रहें और स्वस्थ रहें!";
                case "or" -> "✅ **ବର୍ତ୍ତମାନ କୌଣସି ସକ୍ରିୟ ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ ନାହିଁ।**";
                default -> "✅ **No active outbreak alerts at this time.**\n\nStay cautious and maintain hygiene!";
            };
        }
        StringBuilder sb = new StringBuilder("🚨 **Active Outbreak Alerts in Odisha**\n\n");
        alerts.stream().limit(3).forEach(a -> {
            sb.append("⚠️ **").append(a.getDisease()).append("** — ").append(a.getDistrict())
                    .append("\n").append(a.getDescription().substring(0, Math.min(80, a.getDescription().length())))
                    .append("...\n").append("Severity: ").append(a.getSeverity()).append("\n\n");
        });
        sb.append("Check the Alerts page for full details.");
        return sb.toString();
    }

    private String getMalariaResponse(String lang) {
        return switch (lang) {
            case "hi" -> "🦟 **मलेरिया**\n\n**लक्षण:** ठंड लगकर बुखार, सिरदर्द, उल्टी, शरीर दर्द\n\n" +
                    "**रोकथाम:**\n• मच्छरदानी का उपयोग करें\n• मच्छर रोधक क्रीम लगाएं\n• पानी जमा न होने दें\n\n" +
                    "**उपचार:** डॉक्टर से मिलें, एंटीमलेरियल दवाएं लें।";
            case "or" -> "🦟 **ମ୍ୟାଲେରିଆ**\n\n**ଲକ୍ଷଣ:** ଥଣ୍ଡା ଲାଗି ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ବାନ୍ତି\n\n" +
                    "**ପ୍ରତିରୋଧ:**\n• ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ\n• ଜଳ ଜମିବାକୁ ଦିଅନ୍ତୁ ନାହିଁ\n\n" +
                    "**ଚିକିତ୍ସା:** ଡାକ୍ତରଙ୍କ ପରାମର୍ଶ ନିଅନ୍ତୁ।";
            default -> "🦟 **Malaria**\n\n**Caused by:** Plasmodium parasite via mosquito bite\n\n" +
                    "**Symptoms:**\n• High fever with chills & shivering\n• Severe headache\n• Nausea/vomiting\n• Muscle pain & fatigue\n\n"
                    +
                    "**Prevention:**\n• Sleep under insecticide-treated nets\n• Use mosquito repellent\n• Eliminate standing water\n• Wear full-sleeve clothing at dusk\n\n"
                    +
                    "**Treatment:** Consult a doctor immediately. Free treatment at government hospitals.";
        };
    }

    private String getDengueResponse(String lang) {
        return switch (lang) {
            case "hi" ->
                "🦟 **डेंगू बुखार**\n\n**लक्षण:** तेज बुखार, तीव्र सिरदर्द, आँखों में दर्द, शरीर पर चकत्ते\n\n" +
                        "**रोकथाम:**\n• दिन में भी मच्छरदानी का उपयोग करें\n• कूलर और बर्तनों का पानी बदलते रहें\n\n" +
                        "**⚠️ तुरंत डॉक्टर से मिलें यदि प्लेटलेट्स कम हों।**";
            case "or" -> "🦟 **ଡେଙ୍ଗୁ ଜ୍ୱର**\n\n**ଲକ୍ଷଣ:** ତୀବ୍ର ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ଚର୍ମ ଉପରେ ଦାଗ\n\n" +
                    "**ପ୍ରତିରୋଧ:** ଜଳ ସଂଗ୍ରହ ସ୍ଥାନ ଢ଼ାଙ୍କି ରଖନ୍ତୁ, ମଶା ଦୂର କରନ୍ତୁ।";
            default -> "🦟 **Dengue Fever**\n\n**Caused by:** Dengue virus via Aedes mosquito (day-biting)\n\n" +
                    "**Symptoms:**\n• Sudden high fever (104°F+)\n• Severe headache & eye pain\n• Skin rash\n• Bleeding gums/nose (severe cases)\n• Low platelet count\n\n"
                    +
                    "**Prevention:**\n• Remove stagnant water from coolers, pots, tyres\n• Use mosquito nets during day too\n• Wear protective clothing\n\n"
                    +
                    "**⚠️ Warning:** Seek immediate medical care if fever persists >3 days.";
        };
    }

    private String getTBResponse(String lang) {
        return switch (lang) {
            case "hi" ->
                "🫁 **तपेदिक (TB)**\n\n**लक्षण:** 2 सप्ताह से अधिक खांसी, बुखार, रात को पसीना, वजन कम होना\n\n" +
                        "**इलाज:** DOTS (Directly Observed Treatment) — मुफ्त सरकारी अस्पतालों में!\n\n" +
                        "**✅ TB पूरी तरह ठीक हो सकता है। पूरा कोर्स करें!**";
            case "or" -> "🫁 **ଯକ୍ଷ୍ମା (TB)**\n\n**ଲକ୍ଷଣ:** ୨ ସପ୍ତାହରୁ ଅଧିକ ଖାଂସି, ଜ୍ୱର, ରାତ୍ରି ଘାମ\n\n" +
                    "**ଚିକିତ୍ସା:** DOTS — ସରକାରୀ ଅସ୍ପତାଳରେ ମାଗଣା!\n\n" +
                    "**✅ TB ସଂପୂର୍ଣ ଭଲ ହୋଇ ପାରେ।**";
            default -> "🫁 **Tuberculosis (TB)**\n\n**Caused by:** *Mycobacterium tuberculosis* (airborne)\n\n" +
                    "**Symptoms:**\n• Persistent cough >2 weeks\n• Blood in sputum\n• Fever & night sweats\n• Unexplained weight loss\n• Chest pain\n\n"
                    +
                    "**Prevention:**\n• BCG vaccine for newborns\n• Good ventilation\n• Avoid close contact with TB patients\n\n"
                    +
                    "**Treatment:** FREE DOTS therapy at all government health centres.\n✅ TB is completely curable with full treatment course!";
        };
    }

    private String getCholeraResponse(String lang) {
        return switch (lang) {
            case "hi" -> "💧 **हैजा (Cholera)**\n\n**लक्षण:** पानी जैसा दस्त, उल्टी, मांसपेशियों में ऐंठन\n\n" +
                    "**रोकथाम:**\n• शुद्ध पानी पिएं\n• खाना ढककर रखें\n• ORS घोल से तुरंत इलाज करें";
            case "or" -> "💧 **କଲେରା**\n\n**ଲକ୍ଷଣ:** ଜଳ ଭଳି ଝାଡ଼ା, ବାନ୍ତି\n\n" +
                    "**ପ୍ରତିରୋଧ:** ବିଶୁଦ୍ଧ ଜଳ ପିଅନ୍ତୁ, ORS ଦ୍ରବଣ ପ୍ରଦାନ କରନ୍ତୁ।";
            default -> "💧 **Cholera**\n\n**Caused by:** *Vibrio cholerae* bacteria via contaminated water\n\n" +
                    "**Symptoms:**\n• Profuse watery diarrhea (rice water stools)\n• Severe vomiting\n• Dehydration & muscle cramps\n• Can be fatal within hours if untreated\n\n"
                    +
                    "**Prevention:**\n• Drink boiled/treated water\n• Cover food\n• Wash hands before eating\n• Proper sanitation\n\n"
                    +
                    "**Treatment:** ORS (Oral Rehydration Solution) immediately + IV fluids if severe. Seek medical care urgently!";
        };
    }

    private String getCovidResponse(String lang) {
        return switch (lang) {
            case "hi" -> "😷 **COVID-19**\n\n**लक्षण:** बुखार, खांसी, सांस लेने में तकलीफ, स्वाद/गंध न आना\n\n" +
                    "**रोकथाम:**\n• मास्क पहनें\n• 6 फीट दूरी बनाएं\n• हाथ धोएं\n• टीकाकरण कराएं\n\n" +
                    "**Helpline:** 104";
            case "or" -> "😷 **COVID-19**\n\n**ଲକ୍ଷଣ:** ଜ୍ୱର, ଖାଂସି, ଶ୍ୱାସ ସମସ୍ୟା\n\n" +
                    "**ପ୍ରତିରୋଧ:** ମାସ୍କ ପିନ୍ଧନ୍ତୁ, ଟୀକା ନିଅନ୍ତୁ।\n\n" +
                    "**Helpline:** 104";
            default ->
                "😷 **COVID-19 (Coronavirus)**\n\n**Symptoms:**\n• Fever & chills\n• Cough & shortness of breath\n• Loss of taste/smell\n• Fatigue & body ache\n\n"
                        +
                        "**Prevention:**\n• Wear mask in crowded places\n• Maintain 6-feet distance\n• Wash hands frequently\n• Get vaccinated (COVID vaccine)\n\n"
                        +
                        "**Helpline:** 1075 (National) | 104 (Odisha)\n**Testing:** Free RT-PCR at government hospitals";
        };
    }

    private String getTyphoidResponse(String lang) {
        return switch (lang) {
            case "hi" -> "🌡️ **टाइफाइड**\n\n**लक्षण:** धीरे-धीरे तेज बुखार, सिरदर्द, पेट दर्द, कब्ज या दस्त\n\n" +
                    "**रोकथाम:** शुद्ध पानी, साफ भोजन, हाथ धोना";
            case "or" -> "🌡️ **ଟାଇଫଏଡ**\n\n**ଲକ୍ଷଣ:** ଧୀରେ ବଢ଼ୁଥିବା ଜ୍ୱର, ପେଟ ବ୍ୟଥା\n\n" +
                    "**ପ୍ରତିରୋଧ:** ବିଶୁଦ୍ଧ ଜଳ, ସଫା ଖାଦ୍ୟ।";
            default -> "🌡️ **Typhoid Fever**\n\n**Caused by:** *Salmonella typhi* via contaminated food/water\n\n" +
                    "**Symptoms:**\n• Gradually increasing fever\n• Severe headache\n• Abdominal pain & constipation/diarrhea\n• Rose-coloured rash\n\n"
                    +
                    "**Prevention:**\n• Safe drinking water\n• Proper food hygiene\n• Typhoid vaccine available\n\n" +
                    "**Treatment:** Antibiotics prescribed by doctor. Complete the full course!";
        };
    }

    private String getChildHealthResponse(String lang) {
        return switch (lang) {
            case "hi" -> "👶 **बाल स्वास्थ्य**\n\n" +
                    "**महत्वपूर्ण टीके:**\n• जन्म पर: BCG, पोलियो\n• 6 हफ्ते: DPT, हेपेटाइटिस\n• 9 माह: खसरा (MR)\n\n" +
                    "**⚠️ यदि बच्चे को तेज बुखार, दस्त, या सांस लेने में तकलीफ हो तो तुरंत डॉक्टर के पास जाएं।**";
            case "or" -> "👶 **ଶିଶୁ ସ୍ୱାସ୍ଥ୍ୟ**\n\n" +
                    "**ଜରୁରୀ ଟୀକା:** BCG, ପୋଲିଓ, DPT, MR\n\n" +
                    "**⚠️ ଶିଶୁର ତୀବ୍ର ଜ୍ୱର ବା ଶ୍ୱାସ ସମସ୍ୟା ହେଲେ ତୁରନ୍ତ ଡାକ୍ତରଙ୍କ ପାଖକୁ ଯାଆନ୍ତୁ।**";
            default -> "👶 **Child Health & Immunization**\n\n" +
                    "**Vaccination Schedule:**\n• Birth: BCG, OPV, Hepatitis-B\n• 6 weeks: DPT, IPV, Hib\n• 9 months: MR (Measles-Rubella)\n• 16-24 months: DPT booster\n\n"
                    +
                    "**Signs of danger in children:**\n• High fever (>102°F)\n• Difficulty breathing\n• Severe diarrhea/dehydration\n• Unconsciousness\n\n"
                    +
                    "🏥 **Nearest PHC provides FREE child health services under RBSK programme.**";
        };
    }

    private String getEmergencyResponse(String lang) {
        return switch (lang) {
            case "hi" -> "🚑 **आपातकालीन नंबर**\n\n" +
                    "• **एम्बुलेंस:** 108\n• **स्वास्थ्य हेल्पलाइन:** 104\n" +
                    "• **COVID:** 1075\n• **पुलिस:** 100\n\n" +
                    "**⚠️ यदि आपको तुरंत मदद चाहिए, 108 पर कॉल करें!**";
            case "or" -> "🚑 **ଜରୁରୀ ନଂବର**\n\n" +
                    "• **ଆ୍ୟାମ୍ବୁଲାନ୍ସ:** 108\n• **ସ୍ୱାସ୍ଥ୍ୟ ହେଲ୍ପଲାଇନ:** 104\n" +
                    "• **ପୋଲିସ:** 100";
            default -> "🚑 **Emergency Contact Numbers (Odisha)**\n\n" +
                    "• **Ambulance:** 108 (FREE)\n• **Health Helpline:** 104\n" +
                    "• **COVID-19:** 1075\n• **Police:** 100\n• **Fire:** 101\n\n" +
                    "**⚠️ For medical emergencies, call 108 immediately!**\n\n" +
                    "Nearest hospital info available at: [Odisha Health Portal](https://health.odisha.gov.in)";
        };
    }

    private String getByeResponse(String lang) {
        return switch (lang) {
            case "hi" -> pick(List.of(
                "😊 **धन्यवाद!** स्वस्थ रहें, सुरक्षित रहें! 🙏",
                "👋 **अपना ख्याल रखें!** कभी भी सवाल हो, मुझसे पूछें। 🙏",
                "🌟 **शुभकामनाएं!** हाथ धोएं, साफ पानी पिएं, टीका लगवाएं! 💪"
            ));
            case "or" -> pick(List.of(
                "😊 **ଧନ୍ୟବାଦ!** ସୁସ୍ଥ ଥାଆନ୍ତୁ, ସୁରକ୍ଷିତ ଥାଆନ୍ତୁ! 🙏",
                "👋 **ନିଜ ଯତ୍ନ ନିଅନ୍ତୁ!** ଯେକୌଣସି ପ୍ରଶ୍ନ ଥିଲେ ପଚାରନ୍ତୁ। 🙏"
            ));
            default -> pick(List.of(
                "😊 **Thank you for using the Health Chatbot!**\n\nStay healthy, stay safe! 🙏\n\nRemember: Prevention is always better than cure!",
                "👋 **Take care and stay well!** If you have more health questions, I'm always here. 🙏",
                "🌟 **Goodbye and stay safe!** Wash hands, drink clean water, get vaccinated — stay strong! 💪"
            ));
        };
    }

    private String getDefaultResponse(String lang) {
        return switch (lang) {
            case "hi" -> pick(List.of(
                "🤔 **मैं आपकी बात ठीक से समझ नहीं पाया।**\n\nआप मुझसे पूछ सकते हैं:\n\n" +
                "• रोग के लक्षण (जैसे: मलेरिया के लक्षण)\n• टीकाकरण\n• प्रकोप अलर्ट\n• रोकथाम",
                "💬 **मुझे समझ नहीं आया — क्या आप दोबारा पूछ सकते हैं?**\n\n" +
                "उदाहरण: *'डेंगू के लक्षण क्या हैं?'* या *'बच्चों के लिए कौन से टीके हैं?'*"
            ));
            case "or" -> pick(List.of(
                "🤔 **ମୁଁ ଆପଣଙ୍କ ପ୍ରଶ୍ନ ଠିକ ବୁଝି ପାରିଲିନି।**\n\nଦୟାକରି ଏଗୁଡ଼ିକ ଜିଜ୍ଞାସା କରନ୍ତୁ:\n\n" +
                "• ରୋଗ ଲକ୍ଷଣ\n• ଟୀକାକରଣ\n• ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ",
                "💬 **ବୁଝି ପାରିଲି ନାହିଁ — ଦୟାକରି ଅଲଗା ଭାବରେ ପ୍ରଶ୍ନ କରନ୍ତୁ।**\n\n" +
                "ଉଦାହରଣ: *'ଡେଙ୍ଗୁ ଲକ୍ଷଣ କ'ଣ?'*"
            ));
            default -> pick(List.of(
                "🤔 **I didn't quite catch that — let me help guide you!**\n\n" +
                "You can ask me about:\n\n" +
                "• Disease **symptoms** (e.g., 'What are symptoms of malaria?')\n" +
                "• **Vaccines** and immunization schedules\n" +
                "• **Outbreak alerts** in your area\n" +
                "• **Prevention** tips\n• **Emergency** contacts\n\n" +
                "Try rephrasing your question! 😊",
                "💬 **Hmm, I'm not sure I understood that.**\n\n" +
                "Here are some things I'm great at:\n" +
                "• *'What diseases cause fever and chills?'*\n" +
                "• *'Tell me about dengue prevention'*\n" +
                "• *'What vaccines are free for children?'*\n\n" +
                "Give it a try! 🌟"
            ));
        };
    }

    public List<String> getSuggestions(String intent, String lang) {
        return switch (intent) {
            case "greeting", "general_query" -> Arrays.asList(
                    "Symptoms of Malaria", "Vaccine Schedule", "Active Alerts", "Dengue Prevention");
            case "disease_symptoms", "malaria", "dengue", "tuberculosis", "cholera" -> Arrays.asList(
                    "How to prevent it?", "Vaccine available?", "Nearest hospital", "Emergency contacts");
            case "vaccine" -> Arrays.asList(
                    "Child vaccination", "COVID vaccine", "Typhoid vaccine", "When to vaccinate?");
            case "outbreak_alert" -> Arrays.asList(
                    "Alerts near me", "Malaria cases", "Dengue outbreak", "Stay safe tips");
            default -> Arrays.asList("Disease symptoms", "Vaccination info", "Health alerts", "Prevention tips");
        };
    }
}
