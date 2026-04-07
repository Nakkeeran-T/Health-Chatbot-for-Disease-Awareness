package com.healthchatbot.config;

import com.healthchatbot.entity.DiseaseInfo;
import com.healthchatbot.entity.OutbreakAlert;
import com.healthchatbot.entity.User;
import com.healthchatbot.entity.VaccineSchedule;
import com.healthchatbot.repository.DiseaseInfoRepository;
import com.healthchatbot.repository.OutbreakAlertRepository;
import com.healthchatbot.repository.UserRepository;
import com.healthchatbot.repository.VaccineScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DiseaseInfoRepository diseaseInfoRepository;
    private final VaccineScheduleRepository vaccineRepository;
    private final OutbreakAlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedDiseases();
        seedVaccines();
        seedAlerts();
        log.info("✅ Data seeding completed!");
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@healthodisha.gov.in")
                    .fullName("Admin User")
                    .phone("9876543210")
                    .district("Bhubaneswar")
                    .role(User.Role.ADMIN)
                    .preferredLanguage("en")
                    .build();

            User demo = User.builder()
                    .username("demo")
                    .password(passwordEncoder.encode("demo123"))
                    .email("demo@user.com")
                    .fullName("Demo User")
                    .phone("9876543211")
                    .district("Cuttack")
                    .role(User.Role.USER)
                    .preferredLanguage("en")
                    .build();

            userRepository.saveAll(Arrays.asList(admin, demo));
            log.info("✅ Users seeded: admin/admin123, demo/demo123");
        }
    }

    private void seedDiseases() {
        if (diseaseInfoRepository.count() == 0) {
            diseaseInfoRepository.saveAll(Arrays.asList(
                    DiseaseInfo.builder()
                            .name("Malaria")
                            .category("Vector-borne")
                            .icdCode("B50-B54")
                            .contagious(false)
                            .affectedAgeGroup("All")
                            .description(
                                    "Malaria is a life-threatening disease caused by Plasmodium parasites, transmitted through the bites of infected female Anopheles mosquitoes.")
                            .symptoms(
                                    "High fever with chills and rigors, severe headache, nausea and vomiting, muscle pain and fatigue, sweating, anemia, jaundice in severe cases")
                            .prevention(
                                    "Sleep under insecticide-treated bed nets, use mosquito repellent creams, eliminate stagnant water near homes, wear full-sleeve clothes at dusk, take anti-malarial prophylaxis when travelling to endemic areas")
                            .treatment(
                                    "Artemisinin-based combination therapies (ACT). Free treatment at all government hospitals. Early diagnosis with RDT kits. Consult doctor immediately.")
                            .nameHi("मलेरिया")
                            .symptomsHi(
                                    "ठंड लगकर तेज बुखार, सिरदर्द, मतली और उल्टी, मांसपेशियों में दर्द, थकान, पसीना आना")
                            .preventionHi(
                                    "मच्छरदानी का उपयोग करें, मच्छर भगाने वाली क्रीम लगाएं, घर के पास पानी जमा न होने दें, शाम को पूरे आस्तीन के कपड़े पहनें")
                            .nameOr("ମ୍ୟାଲେରିଆ")
                            .symptomsOr("ଥଣ୍ଡା ଲାଗି ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ବାନ୍ତି, ଶରୀର ଦୁଃଖ, ଥକାପଣ")
                            .preventionOr("ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ, ଜଳ ଜମିବାକୁ ଦିଅନ୍ତୁ ନାହିଁ, ପ୍ରତିଷେଧ ଔଷଧ ଖାଆନ୍ତୁ")
                            .build(),

                    DiseaseInfo.builder()
                            .name("Dengue Fever")
                            .category("Vector-borne")
                            .icdCode("A90-A91")
                            .contagious(false)
                            .affectedAgeGroup("All")
                            .description(
                                    "Dengue is a mosquito-borne viral infection caused by dengue virus transmitted by female Aedes aegypti mosquitoes.")
                            .symptoms(
                                    "Sudden high fever (104°F+), severe headache, pain behind eyes, joint and muscle pain, skin rash, nausea, bleeding gums or nose, low platelet count")
                            .prevention(
                                    "Eliminate stagnant water in coolers, flower pots, tyres, use mosquito nets during day, wear protective clothing, use repellents with DEET, keep surroundings clean and dry")
                            .treatment(
                                    "No specific antiviral treatment. Supportive care with fluids, pain relievers (avoid aspirin). Hospitalisation for severe dengue (dengue hemorrhagic fever). Free care at government hospitals.")
                            .nameHi("डेंगू बुखार")
                            .symptomsHi(
                                    "अचानक तेज बुखार, तीव्र सिरदर्द, आंखों के पीछे दर्द, जोड़ों में दर्द, शरीर पर चकत्ते, मसूड़ों से खून")
                            .preventionHi(
                                    "कूलर, गमले, टायर में पानी जमा न होने दें, दिन में भी मच्छरदानी का उपयोग करें, पूरे कपड़े पहनें")
                            .nameOr("ଡେଙ୍ଗୁ ଜ୍ୱର")
                            .symptomsOr("ହଠାତ ତୀବ୍ର ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ଚର୍ମ ଦାଗ, ରକ୍ତସ୍ରାବ")
                            .preventionOr("ଜଳ ସଂଗ୍ରହ ସ୍ଥାନ ଢ଼ାଙ୍କି ରଖନ୍ତୁ, ମଶା ନଷ୍ଟ କରନ୍ତୁ")
                            .build(),

                    DiseaseInfo.builder()
                            .name("Tuberculosis")
                            .category("Airborne")
                            .icdCode("A15-A19")
                            .contagious(true)
                            .affectedAgeGroup("All especially adults")
                            .description(
                                    "Tuberculosis (TB) is an infectious bacterial disease caused by Mycobacterium tuberculosis that primarily affects the lungs.")
                            .symptoms(
                                    "Persistent cough lasting more than 2 weeks, coughing up blood, chest pain, unexplained weight loss, fatigue, fever, night sweats, loss of appetite")
                            .prevention(
                                    "BCG vaccination for newborns, ensure good ventilation, avoid close contact with TB patients, complete full course of medication, early diagnosis and treatment")
                            .treatment(
                                    "Free DOTS (Directly Observed Treatment Short-course) therapy at all government health centres. 6-month antibiotic course. Completely curable with proper treatment!")
                            .nameHi("तपेदिक (टीबी)")
                            .symptomsHi(
                                    "2 हफ्ते से ज्यादा खांसी, खांसी में खून, छाती में दर्द, वजन कम होना, रात को पसीना आना")
                            .preventionHi(
                                    "BCG टीका लगवाएं, अच्छा हवादार कमरे में रहें, TB रोगी से दूरी बनाएं, पूरी दवा लें")
                            .nameOr("ଯକ୍ଷ୍ମା (ଟିବି)")
                            .symptomsOr("୨ ସପ୍ତାହରୁ ଅଧିକ ଖାଂସି, ରକ୍ତ ଖାଂସି, ଛାତି ବ୍ୟଥା, ଓଜନ ହ୍ରାସ, ରାତ୍ରି ଘାମ")
                            .preventionOr("BCG ଟୀକା ଦିଅନ୍ତୁ, ଭଲ ଯୋଗ ରଖନ୍ତୁ, ସଂପୂର୍ଣ ଔଷଧ ଖାଆନ୍ତୁ")
                            .build(),

                    DiseaseInfo.builder()
                            .name("Cholera")
                            .category("Waterborne")
                            .icdCode("A00")
                            .contagious(true)
                            .affectedAgeGroup("All")
                            .description(
                                    "Cholera is an acute diarrheal illness caused by Vibrio cholerae bacteria, spread through contaminated water or food.")
                            .symptoms(
                                    "Sudden onset of profuse watery diarrhea (rice water stools), vomiting, rapid dehydration, muscle cramps, may become fatal within hours")
                            .prevention(
                                    "Drink only boiled or treated water, eat properly cooked food, wash hands with soap before eating and after toilet, use pit latrines, proper disposal of sewage")
                            .treatment(
                                    "ORS (Oral Rehydration Solution) immediately, IV fluids for severe cases, antibiotics as prescribed. Free treatment at government hospitals.")
                            .nameHi("हैजा")
                            .symptomsHi("पानी जैसा दस्त, उल्टी, पेट में ऐंठन, तेज निर्जलीकरण")
                            .preventionHi("उबला पानी पिएं, खाने से पहले हाथ धोएं, स्वच्छ शौचालय का उपयोग करें")
                            .nameOr("କଲେରା")
                            .symptomsOr("ଜଳ ଭଳି ଝାଡ଼ା, ବାନ୍ତି, ପେଟ ମୋଡ଼, ଶୀଘ୍ର ଜଳ ହ୍ରାସ")
                            .preventionOr("ଫୁଟାଇ ଥଣ୍ଡା ଜଳ ପିଅନ୍ତୁ, ଖାଦ୍ୟ ପୂର୍ବରୁ ହାତ ଧୁଅନ୍ତୁ")
                            .build(),

                    DiseaseInfo.builder()
                            .name("COVID-19")
                            .category("Respiratory")
                            .icdCode("U07.1")
                            .contagious(true)
                            .affectedAgeGroup("All")
                            .description(
                                    "COVID-19 is an infectious disease caused by the SARS-CoV-2 virus, primarily spreading through respiratory droplets.")
                            .symptoms(
                                    "Fever and chills, dry cough, shortness of breath, fatigue, loss of taste and smell, sore throat, body aches, diarrhea")
                            .prevention(
                                    "Wear mask in crowded places, maintain 6-feet distance, wash hands frequently, get COVID-19 vaccination (Covaxin/Covishield/Corbevax)")
                            .treatment(
                                    "Mild: Home isolation, rest, hydration. Severe: Hospital care, oxygen support, antiviral drugs. Free treatment at government hospitals. Helpline: 1075")
                            .nameHi("कोविड-19")
                            .symptomsHi("बुखार, सूखी खांसी, सांस लेने में तकलीफ, थकान, स्वाद और गंध न आना")
                            .preventionHi("मास्क पहनें, 6 फीट दूरी, हाथ धोएं, टीकाकरण कराएं")
                            .nameOr("କୋଭିଡ-19")
                            .symptomsOr("ଜ୍ୱର, ଖାଂସି, ଶ୍ୱାସ ଅସୁବିଧା, ଥକାପଣ, ସ୍ୱାଦ ଓ ଗନ୍ଧ ଚଲିଯିବା")
                            .preventionOr("ମାସ୍କ ପିନ୍ଧନ୍ତୁ, ଦୂରତ୍ୱ ରଖନ୍ତୁ, ହାତ ଧୁଅନ୍ତୁ, ଟୀକା ନିଅନ୍ତୁ")
                            .build(),

                    DiseaseInfo.builder()
                            .name("Typhoid")
                            .category("Foodborne")
                            .icdCode("A01.0")
                            .contagious(true)
                            .affectedAgeGroup("Children and young adults")
                            .description(
                                    "Typhoid fever is a bacterial infection caused by Salmonella typhi, spread through contaminated food and water.")
                            .symptoms(
                                    "Gradually increasing fever, severe headache, weakness, stomach pain, constipation or diarrhea, rose-coloured spots on skin, loss of appetite")
                            .prevention(
                                    "Typhoid vaccination, drink safe water, eat properly cooked food, wash hands before eating, avoid eating outside food from unhygienic places")
                            .treatment(
                                    "Antibiotics prescribed by doctor. Complete the full antibiotic course. Rest and plenty of fluids. Free treatment at government hospitals.")
                            .nameHi("टाइफाइड")
                            .symptomsHi("धीरे-धीरे बढ़ता बुखार, सिरदर्द, पेट दर्द, कमजोरी, कब्ज या दस्त")
                            .preventionHi("टाइफाइड टीका, शुद्ध पानी और खाना, हाथ धोना")
                            .nameOr("ଟାଇଫଏଡ")
                            .symptomsOr("ଧୀରେ ବଢ଼ୁଥିବା ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ପେଟ ବ୍ୟଥା, ଦୁର୍ବଳତା")
                            .preventionOr("ଟାଇଫଏଡ ଟୀକା, ବିଶୁଦ୍ଧ ଜଳ ଓ ଖାଦ୍ୟ")
                            .build(),
                    DiseaseInfo.builder()
                            .name("Scrub Typhus")
                            .category("Rickettsial")
                            .icdCode("A75.3")
                            .contagious(false)
                            .affectedAgeGroup("All")
                            .description("Scrub typhus is a disease caused by Orientia tsutsugamushi bacteria, spread to people through bites of infected larval mites (chiggers). Common in hilly/forested areas of Odisha.")
                            .symptoms("High fever, headache, body aches, a dark scab-like sore (eschar) at the bite site, enlarged lymph nodes, mental changes (mild to severe)")
                            .prevention("Avoid walking through tall grass or scrub, wear long sleeves and pants, apply insect repellent, maintain environmental hygiene around houses")
                            .treatment("Doxycycline (antibiotic) is choice treatment. Must be started early based on clinical suspicion. Available at government hospitals.")
                            .nameHi("स्क्रब टाइफस")
                            .nameOr("ସ୍କ୍ରବ୍ ଟାଇଫସ୍")
                            .build(),
                    DiseaseInfo.builder()
                            .name("Japanese Encephalitis")
                            .category("Viral/Vector-borne")
                            .icdCode("A83.0")
                            .contagious(false)
                            .affectedAgeGroup("Children (mostly <15 years)")
                            .description("Japanese encephalitis (JE) is a viral brain infection spread by Culex mosquitoes. It is serious and can lead to inflammation of the brain.")
                            .symptoms("Fever, headache, vomiting, mental status changes, neurological symptoms, weakness, movement disorders, seizures (especially in children)")
                            .prevention("JE vaccination is part of Universal Immunization Program, use mosquito nets, wear protective clothing, reduce mosquito breeding in pig farms and rice fields")
                            .treatment("No specific antiviral treatment. Supportive care in hospital. Vaccination is the best prevention. Free at government centres.")
                            .nameHi("जापानी इंसेफेलाइटिस")
                            .nameOr("ଜାପାନୀ ଏନସେଫାଲାଇଟିସ୍")
                            .build()));
            log.info("✅ Diseases seeded: 6 diseases");
        }
    }

    private void seedVaccines() {
        if (vaccineRepository.count() == 0) {
            vaccineRepository.saveAll(Arrays.asList(
                    VaccineSchedule.builder()
                            .vaccineName("BCG (Bacillus Calmette-Guérin)")
                            .description("Protects against Tuberculosis (TB). Given at birth.")
                            .targetAge("At Birth")
                            .doseSchedule("Single dose at birth")
                            .disease("Tuberculosis")
                            .numberOfDoses(1)
                            .administrationRoute("Intradermal injection")
                            .mandatoryUnderNHM(true)
                            .availability("Free at all government health centres")
                            .vaccineNameHi("BCG टीका")
                            .vaccineNameOr("BCG ଟୀକା")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("OPV (Oral Polio Vaccine)")
                            .description("Protects against Poliomyelitis. Oral drops.")
                            .targetAge("At Birth, 6 weeks, 10 weeks, 14 weeks, 16-24 months")
                            .doseSchedule("At birth + at 6, 10, 14 weeks + booster at 16-24 months")
                            .disease("Polio")
                            .numberOfDoses(5)
                            .administrationRoute("Oral drops")
                            .mandatoryUnderNHM(true)
                            .availability("Free at all government health centres & National Immunization Day")
                            .vaccineNameHi("ओरल पोलियो वैक्सीन")
                            .vaccineNameOr("ଓରାଲ ପୋଲିଓ ଭ୍ୟାକ୍ସିନ")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("Hepatitis-B Vaccine")
                            .description("Protects against Hepatitis B liver infection.")
                            .targetAge("At Birth, 6 weeks, 10 weeks, 14 weeks")
                            .doseSchedule("Birth dose + at 6, 10, 14 weeks")
                            .disease("Hepatitis B")
                            .numberOfDoses(4)
                            .administrationRoute("Intramuscular injection")
                            .mandatoryUnderNHM(true)
                            .availability("Free at all government health centres")
                            .vaccineNameHi("हेपेटाइटिस-बी टीका")
                            .vaccineNameOr("ହେପାଟାଇଟିସ-ବି ଟୀକା")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("DPT (Diphtheria-Pertussis-Tetanus)")
                            .description(
                                    "Combination vaccine protecting against Diphtheria, Whooping Cough, and Tetanus.")
                            .targetAge("6 weeks, 10 weeks, 14 weeks, 16-24 months, 5-6 years")
                            .doseSchedule("Primary: 6, 10, 14 weeks | Booster 1: 16-24 months | Booster 2: 5-6 years")
                            .disease("Diphtheria, Pertussis, Tetanus")
                            .numberOfDoses(5)
                            .administrationRoute("Intramuscular injection")
                            .mandatoryUnderNHM(true)
                            .availability("Free at all government health centres")
                            .vaccineNameHi("DPT टीका")
                            .vaccineNameOr("DPT ଟୀକା")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("MR (Measles-Rubella) Vaccine")
                            .description("Protects against Measles (Khasra) and Rubella (German Measles).")
                            .targetAge("9-12 months, 16-24 months")
                            .doseSchedule("First dose: 9-12 months | Second dose: 16-24 months")
                            .disease("Measles, Rubella")
                            .numberOfDoses(2)
                            .administrationRoute("Subcutaneous injection")
                            .mandatoryUnderNHM(true)
                            .availability("Free at all government health centres")
                            .vaccineNameHi("MR (खसरा-रूबेला) टीका")
                            .vaccineNameOr("MR ଟୀକା")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("COVID-19 Vaccine (Covaxin/Covishield)")
                            .description("Protects against COVID-19 coronavirus disease. Multiple vaccines available.")
                            .targetAge("18 years and above (12+ for Corbevax)")
                            .doseSchedule("Primary 2 doses + Booster dose")
                            .disease("COVID-19")
                            .numberOfDoses(3)
                            .administrationRoute("Intramuscular injection")
                            .mandatoryUnderNHM(false)
                            .availability("Free at government vaccination centres, CoWIN registration required")
                            .vaccineNameHi("कोविड-19 वैक्सीन")
                            .vaccineNameOr("କୋଭିଡ-19 ଭ୍ୟାକ୍ସିନ")
                            .build(),

                    VaccineSchedule.builder()
                            .vaccineName("Typhoid Conjugate Vaccine (TCV)")
                            .description("Protects against Typhoid fever.")
                            .targetAge("9 months and above")
                            .doseSchedule("Single dose at 9 months, booster at 2 years")
                            .disease("Typhoid")
                            .numberOfDoses(2)
                            .administrationRoute("Intramuscular injection")
                            .mandatoryUnderNHM(true)
                            .availability("Free at government health centres")
                            .vaccineNameHi("टाइफाइड टीका")
                            .vaccineNameOr("ଟାଇଫଏଡ ଟୀକା")
                            .build()));
            log.info("✅ Vaccines seeded: 7 vaccines");
        }
    }

    private void seedAlerts() {
        if (alertRepository.count() == 0) {
            alertRepository.saveAll(Arrays.asList(
                    OutbreakAlert.builder()
                            .title("Dengue Outbreak Alert - Bhubaneswar")
                            .description(
                                    "Rising dengue cases reported in Bhubaneswar urban areas. Citizens advised to eliminate stagnant water and use mosquito repellents. Health teams deployed.")
                            .disease("Dengue")
                            .region("Odisha")
                            .district("Khordha")
                            .severity(OutbreakAlert.SeverityLevel.HIGH)
                            .reportedCases(142)
                            .active(true)
                            .precautions(
                                    "Eliminate stagnant water, use mosquito nets, wear full-sleeve clothing, consult doctor if fever persists")
                            .contactNumber("104")
                            .titleHi("डेंगू प्रकोप अलर्ट - भुवनेश्वर")
                            .titleOr("ଡେଙ୍ଗୁ ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ - ଭୁବନେଶ୍ୱର")
                            .descriptionHi("भुवनेश्वर में डेंगू के मामले बढ़ रहे हैं। नागरिकों को सतर्क रहने की सलाह।")
                            .descriptionOr("ଭୁବନେଶ୍ୱରରେ ଡେଙ୍ଗୁ ରୋଗ ବଢ଼ୁଛି। ନାଗରିକ ମାନଙ୍କୁ ସତର୍କ ଥିବାକୁ ଅନୁରୋଧ।")
                            .build(),

                    OutbreakAlert.builder()
                            .title("Malaria Alert - Koraput District")
                            .description(
                                    "Increased malaria cases reported in tribal areas of Koraput. NVBDCP teams conducting indoor residual spraying (IRS). Free RDT testing available at PHCs.")
                            .disease("Malaria")
                            .region("Odisha")
                            .district("Koraput")
                            .severity(OutbreakAlert.SeverityLevel.MEDIUM)
                            .reportedCases(78)
                            .active(true)
                            .precautions(
                                    "Sleep under treated nets, take anti-malarial tablets, report fever immediately, attend IRS spraying programme")
                            .contactNumber("104")
                            .titleHi("मलेरिया अलर्ट - कोरापुट जिला")
                            .titleOr("ମ୍ୟାଲେରିଆ ଆଲର୍ଟ - କୋରାପୁଟ")
                            .build(),

                    OutbreakAlert.builder()
                            .title("Cholera Prevention Advisory - Puri")
                            .description(
                                    "Post-flood cholera risk in low-lying areas of Puri district. Drinking water sources may be contaminated. Use only boiled/purified water.")
                            .disease("Cholera")
                            .region("Odisha")
                            .district("Puri")
                            .severity(OutbreakAlert.SeverityLevel.MEDIUM)
                            .reportedCases(23)
                            .active(true)
                            .precautions(
                                    "Boil water before drinking, use ORS for diarrhea, wash hands frequently, avoid eating street food")
                            .contactNumber("104")
                            .titleHi("हैजा रोकथाम सलाह - पुरी")
                            .titleOr("କଲେରା ସତର୍କ - ପୁରୀ")
                            .build()));
            log.info("✅ Alerts seeded: 3 active outbreak alerts");
        }
    }
}
