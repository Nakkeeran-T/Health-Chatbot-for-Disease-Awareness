package com.healthchatbot.service;

import com.healthchatbot.entity.DiseaseInfo;
import com.healthchatbot.entity.VaccineSchedule;
import com.healthchatbot.repository.DiseaseInfoRepository;
import com.healthchatbot.repository.VaccineScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Government Health Data Service
 *
 * Integrates authoritative health data from:
 * - India's National Health Mission (NHM) — vaccination schedules
 * - National Centre for Disease Control (NCDC) — disease info & ICD codes
 * - Odisha State Health & Family Welfare Department — region-specific data
 *
 * In a production deployment, this service would call live REST APIs from these
 * government portals. For this implementation, it pre-seeds the PostgreSQL database
 * with the official data as a structured mock of those APIs, so the chatbot serves
 * authoritative information rather than hardcoded strings.
 *
 * Data sources:
 *  - https://nhm.odisha.gov.in (Odisha NHM)
 *  - https://ncdc.mohfw.gov.in (NCDC disease surveillance)
 *  - https://mohfw.gov.in (Ministry of Health & Family Welfare)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentHealthDataService {

    private final DiseaseInfoRepository diseaseInfoRepository;
    private final VaccineScheduleRepository vaccineScheduleRepository;

    // ─── Disease Data (from NCDC / MOHFW sources) ──────────────────────────────

    @Transactional
    public void seedDiseaseData() {
        if (diseaseInfoRepository.count() > 0) {
            log.info("✅ Disease database already seeded ({} records). Skipping.", diseaseInfoRepository.count());
            return;
        }

        log.info("🌱 Seeding government disease database from NCDC/NHM data...");

        List<DiseaseInfo> diseases = List.of(

            // ── MALARIA ─────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Malaria")
                .icdCode("B50-B54")
                .category("Vector-Borne Disease")
                .description("Malaria is a life-threatening disease caused by Plasmodium parasites transmitted through the bites of infected female Anopheles mosquitoes. Odisha accounts for ~20% of India's total malaria burden.")
                .symptoms("High fever with chills, headache, muscle aches, nausea, vomiting, sweating, fatigue. Fever follows a cyclic pattern every 48-72 hours.")
                .prevention("Use insecticide-treated bed nets (LLIN) every night. Eliminate stagnant water around home. Use mosquito repellent. Indoor Residual Spraying (IRS) by ANM. Take chemoprophylaxis if prescribed.")
                .treatment("FREE antimalarial treatment at all Government hospitals and PHCs. Chloroquine for P.vivax; Artemisinin Combination Therapy (ACT) for P.falciparum. Full course must be completed. Call 104.")
                .contagious(false)
                .affectedAgeGroup("All ages; children under 5 and pregnant women at highest risk")
                .nameHi("मलेरिया")
                .symptomsHi("तेज़ बुखार, ठंड लगना, सिरदर्द, मांसपेशियों में दर्द, उल्टी, थकान — बुखार हर 48-72 घंटे में आता है।")
                .preventionHi("मच्छरदानी के नीचे सोएं। पानी जमा न होने दें। मच्छर भगाने वाली क्रीम लगाएं। ANM से IRS के लिए कहें।")
                .nameOr("ମ୍ୟାଲେରିଆ")
                .symptomsOr("ଜ୍ୱର, ଥଣ୍ଡା, ମୁଣ୍ଡ ବ୍ୟଥା, ବାନ୍ତି, ଥକ୍କାଣ — ଜ୍ୱର ପ୍ରତି 48-72 ଘଣ୍ଟାରେ ଆସେ।")
                .preventionOr("ରାତ୍ରେ ମଶାରୀ ତଳେ ଶୁଅନ୍ତୁ। ଜଳ ଜମା ହେବାକୁ ଦିଅନ୍ତୁ ନାହିଁ। ANM ଙ୍କ ଠାରୁ IRS ‌ ପ୍ରୟୋଗ ଆବଶ୍ୟକ।")
                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Malaria.jpg/320px-Malaria.jpg")
                .build(),

            // ── DENGUE ──────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Dengue")
                .icdCode("A90-A91")
                .category("Vector-Borne Disease")
                .description("Dengue fever is a mosquito-borne tropical disease caused by the dengue virus. Aedes aegypti mosquito bites primarily during the day. Severe dengue (dengue haemorrhagic fever) can be fatal.")
                .symptoms("Sudden high fever (40°C/104°F), severe headache, pain behind eyes, joint/muscle pain, skin rash, mild bleeding (nose/gums), low platelet count.")
                .prevention("Remove standing water from pots, coolers, tyres, flower vases. Use mosquito nets even during the day. Wear full-sleeved clothes. Use mosquito repellent. Keep drains clean.")
                .treatment("No specific antiviral drug. Rest and drink plenty of fluids. Paracetamol for fever — AVOID aspirin/ibuprofen. Hospitalise immediately if platelet count drops below 1 lakh. Call 104.")
                .contagious(false)
                .affectedAgeGroup("All ages")
                .nameHi("डेंगू")
                .symptomsHi("तेज बुखार, सिरदर्द, आँखों के पीछे दर्द, शरीर दर्द, चकत्ते, नाक/मसूड़ों से खून।")
                .preventionHi("कूलर, बर्तन में पानी जमा न होने दें। दिन में भी मच्छरदानी लगाएं। पूरे कपड़े पहनें।")
                .nameOr("ଡେଙ୍ଗୁ")
                .symptomsOr("ଅଚାନକ ଜ୍ୱର, ମୁଣ୍ଡ ବ୍ୟଥା, ଆଖ ପଛରେ ଯନ୍ତ୍ରଣା, ଚର୍ମ ଦାଗ, ନାକ/ଦାନ୍ତ ରକ୍ତ।")
                .preventionOr("ଥালି, କୁଲର, ଟାୟରରେ ପାଣି ଜମା ହେବାକୁ ଦିଅନ୍ତୁ ନାହିଁ। ଦିନ ସମୟରେ ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ।")
                .build(),

            // ── TUBERCULOSIS ─────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Tuberculosis")
                .icdCode("A15-A19")
                .category("Airborne Disease")
                .description("TB is caused by Mycobacterium tuberculosis and spread through the air when an infected person coughs or sneezes. India has the highest TB burden in the world. Run under RNTCP/NTP programme.")
                .symptoms("Persistent cough for 2+ weeks, coughing blood, night sweats, unexplained weight loss, low-grade fever, fatigue, chest pain, loss of appetite.")
                .prevention("BCG vaccine at birth. Good ventilation. Don't spit in public. Cover mouth when coughing. Avoid close contact with active TB patients. Get tested if in contact with a TB patient.")
                .treatment("FREE DOTS (Directly Observed Treatment Short-course) at all Government PHCs and hospitals under National TB Elimination Programme (NTEP). 6-month drug regimen — MUST complete the full course. Nikshay helpline: 1800-116-666")
                .contagious(true)
                .affectedAgeGroup("All ages; most common in adults 15-55 years")
                .nameHi("तपेदिक / टीबी")
                .symptomsHi("2 हफ्ते से अधिक खांसी, बलगम में खून, रात को पसीना, वजन कम होना, थकान।")
                .preventionHi("जन्म के समय BCG टीका लगवाएं। खांसते समय मुँह ढकें। सार्वजनिक जगह में न थूकें।")
                .nameOr("ଯକ୍ଷ୍ମା / ଟିବି")
                .symptomsOr("2 ସପ୍ତାହ ହୋଇ ଖାଂସି, ରକ୍ତ ଥୁ, ରାତ୍ରି ଘାମ, ଓଜନ ହ୍ରାସ, ଥକ୍କାଣ।")
                .preventionOr("ଜନ୍ମ ସମୟରେ BCG ଟୀକା ନିଅନ୍ତୁ। ଖାଂସି ସମୟରେ ମୁଖ ଘୋଡ଼ନ୍ତୁ।")
                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/TB_poster.jpg/320px-TB_poster.jpg")
                .build(),

            // ── CHOLERA ──────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Cholera")
                .icdCode("A00")
                .category("Water-Borne Disease")
                .description("Cholera is an acute diarrhoeal disease caused by Vibrio cholerae bacteria, typically through contaminated water or food. It can cause severe dehydration and death within hours if untreated.")
                .symptoms("Profuse watery diarrhoea (rice-water stools), severe vomiting, rapid dehydration, muscle cramps, sunken eyes, dry mouth, rapid heart rate.")
                .prevention("Drink only boiled or chlorinated water. Wash hands with soap before eating and after using the toilet. Eat freshly cooked food. Keep surroundings and toilet clean. Use ORS solution.")
                .treatment("ORS (Oral Rehydration Solution) immediately. IV fluids if severely dehydrated. Antibiotics as per doctor's advice. Rush to nearest hospital. Available FREE at all PHCs. Call 104/108.")
                .contagious(true)
                .affectedAgeGroup("All ages; children under 5 higher risk")
                .nameHi("हैजा")
                .symptomsHi("पानी जैसे दस्त, उल्टी, तेज़ निर्जलीकरण, मांसपेशियों में ऐंठन, कमज़ोरी।")
                .preventionHi("उबला हुआ पानी पिएं। खाने से पहले और शौच के बाद हाथ धोएं। ताज़ा पका हुआ खाना खाएं।")
                .nameOr("ବିଷୂଚିକା / କଲେରା")
                .symptomsOr("ତରଳ ଝାଡ଼ା, ବାନ୍ତି, ଦ୍ରୁତ ନିର୍ଜଳ, ଖଞ୍ଜ, ଦୁର୍ବଳତା।")
                .preventionOr("ଫୁଟା ପାଣି ପିଅନ୍ତୁ। ଖାଇବା ଆଗରୁ ଓ ଶୌଚ ପରେ ହାତ ଧୁଅନ୍ତୁ।")
                .build(),

            // ── COVID-19 ─────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("COVID-19")
                .icdCode("U07.1")
                .category("Respiratory Disease")
                .description("COVID-19 is caused by SARS-CoV-2 coronavirus, spread primarily through respiratory droplets. Managed under India's Integrated Disease Surveillance Programme (IDSP).")
                .symptoms("Fever, dry cough, fatigue, loss of taste/smell, sore throat, body aches, shortness of breath, headache. Severe: breathing difficulty, chest pain, confusion.")
                .prevention("Wear N95/triple-layer mask in crowds. Maintain 6-feet distance. Wash hands frequently. Get vaccinated with approved vaccines (Covishield, Covaxin, Corbevax). Avoid crowded and poorly ventilated places.")
                .treatment("Isolate at home for mild cases. Paracetamol for fever. Rest and fluids. Call 1075 (National COVID helpline) or 104 (Odisha) if breathing difficulty. DCHC/DCH for moderate/severe cases.")
                .contagious(true)
                .affectedAgeGroup("All ages; elderly and immunocompromised at highest risk")
                .nameHi("कोविड-19")
                .symptomsHi("बुखार, खांसी, थकान, स्वाद/गंध न आना, सांस में तकलीफ, सिरदर्द।")
                .preventionHi("मास्क पहनें। 6 फुट की दूरी बनाएं। बार-बार हाथ धोएं। COVID टीका लगवाएं।")
                .nameOr("କୋଭିଡ-19")
                .symptomsOr("ଜ୍ୱର, ଖାଂସି, ଥକ୍କାଣ, ସ୍ୱାଦ/ଗନ୍ଧ ନ ଲାଗିବା, ଶ୍ୱାସ ସମସ୍ୟା।")
                .preventionOr("ମାସ୍କ ପିନ୍ଧନ୍ତୁ। 6 ଫୁଟ ଦୂରତ୍ୱ ରଖନ୍ତୁ। COVID ଟୀକା ନିଅନ୍ତୁ।")
                .imageUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/SARS-CoV-2_without_background.png/320px-SARS-CoV-2_without_background.png")
                .build(),

            // ── TYPHOID ──────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Typhoid")
                .icdCode("A01")
                .category("Water-Borne Disease")
                .description("Typhoid fever is a systemic infection caused by Salmonella typhi bacteria through contaminated food and water. Common during floods and monsoons in Odisha.")
                .symptoms("Prolonged high fever (often 39-40°C), headache, abdominal pain, constipation or diarrhoea, rose spots on skin, weakness, loss of appetite, slow pulse.")
                .prevention("Typhoid vaccine (Vi polysaccharide) available at PHCs. Safe drinking water only. Wash hands before eating. Avoid street food and raw vegetables. Proper sewage disposal.")
                .treatment("Antibiotics (Azithromycin/Cephalosporins) as prescribed by doctor. Complete the full antibiotic course. Rest and fluids. Available at Government hospitals. Call 104.")
                .contagious(true)
                .affectedAgeGroup("Children 5-19 years most affected")
                .nameHi("टाइफाइड")
                .symptomsHi("लंबे समय तक बुखार, पेट दर्द, सिरदर्द, कब्ज या दस्त, कमज़ोरी।")
                .preventionHi("टाइफाइड का टीका लगवाएं। उबला पानी पिएं। बाहर का खाना न खाएं।")
                .nameOr("ଟାଇଫଏଡ")
                .symptomsOr("ଦୀର୍ଘ ଜ୍ୱର, ଉଦର ଯନ୍ତ୍ରଣା, ମୁଣ୍ଡ ବ୍ୟଥା, ଅବସାଦ।")
                .preventionOr("ଟାଇଫଏଡ ଟୀକା ନିଅନ୍ତୁ। ଫୁଟା ପାଣି ପିଅନ୍ତୁ। ବାହାର ଖାଦ୍ୟ ଖାଆନ୍ତୁ ନାହିଁ।")
                .build(),

            // ── HEPATITIS B ──────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Hepatitis B")
                .icdCode("B16-B19")
                .category("Blood-Borne Disease")
                .description("Hepatitis B is a liver infection caused by the Hepatitis B virus (HBV) transmitted through blood, semen, or other body fluids. India has ~40 million HBV carriers. Hepatitis B vaccine included in Universal Immunization Programme (UIP).")
                .symptoms("Fatigue, loss of appetite, nausea, abdominal pain, dark urine, jaundice (yellow eyes/skin), joint pain. Many are asymptomatic carriers.")
                .prevention("Hepatitis B vaccine (3 doses — at birth, 6 weeks, 14 weeks) under UIP. Avoid sharing needles, razors, or personal items. Safe blood transfusion. Safe sexual practices.")
                .treatment("Antiviral drugs (Tenofovir/Entecavir) for chronic HBV — available at Government hospitals. No cure, but medications suppress viral load. Regular monitoring. Call 104.")
                .contagious(true)
                .affectedAgeGroup("All ages; highest risk — newborns of infected mothers")
                .nameHi("हेपेटाइटिस बी")
                .symptomsHi("थकान, भूख न लगना, पेट दर्द, पीलिया (आँखें/त्वचा पीली), काला पेशाब।")
                .preventionHi("जन्म के समय + 6 हफ्ते + 14 हफ्ते पर Hepatitis B टीका लगवाएं। सुई न बांटें।")
                .nameOr("ହେପାଟାଇଟିସ ବି")
                .symptomsOr("ଥକ୍କାଣ, ଭୋକ ନ ଲାଗିବା, ଉଦର ଯନ୍ତ୍ରଣା, ଜଣ୍ଡିସ, ଗାଢ଼ ପ୍ରସ୍ରାବ।")
                .preventionOr("ଜନ୍ମ + 6 ସପ୍ତାହ + 14 ସପ୍ତାହରେ Hepatitis B ଟୀକା ନିଅନ୍ତୁ।")
                .build(),

            // ── MEASLES ──────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Measles")
                .icdCode("B05")
                .category("Vaccine-Preventable Disease")
                .description("Measles is a highly contagious viral disease transmitted through respiratory droplets. One of the leading causes of death among young children. Eradication target under Mission Indradhanush.")
                .symptoms("High fever, runny nose, red/watery eyes (conjunctivitis), white spots inside mouth (Koplik's spots), skin rash starting on face spreading to body.")
                .prevention("MR (Measles-Rubella) vaccine at 9-12 months and 16-24 months under UIP. MR vaccine is FREE at all PHCs. Keep children's immunization schedule up to date.")
                .treatment("No specific antiviral treatment. Vitamin A supplementation reduces severity. Treat fever with paracetamol. Ensure good nutrition and hydration. Isolate the child. Call 104.")
                .contagious(true)
                .affectedAgeGroup("Children under 5 years primarily")
                .nameHi("खसरा")
                .symptomsHi("तेज बुखार, नाक बहना, आँखें लाल, मुँह में सफेद धब्बे, त्वचा पर दाने।")
                .preventionHi("9 महीने और 16-24 महीने पर MR टीका लगवाएं। सभी Government PHC में मुफ्त।")
                .nameOr("ହାମ")
                .symptomsOr("ଜ୍ୱର, ନାକ ଝରିବା, ଆଖ ଲାଲ, ମୁଖ ଭିତରେ ଧଳା ଦାଗ, ଚର୍ମ ଦାଗ।")
                .preventionOr("9 ମାସ ଓ 16-24 ମାସରେ MR ଟୀକା ନିଅନ୍ତୁ। ସମସ୍ତ PHCରେ ମାଗଣା।")
                .build(),

            // ── INFLUENZA ─────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Influenza")
                .icdCode("J09-J11")
                .category("Respiratory Disease")
                .description("Seasonal influenza (flu) is caused by influenza viruses A, B, or C. Spread through respiratory droplets. High-risk groups include elderly, children, pregnant women, and healthcare workers.")
                .symptoms("Sudden fever, severe body ache, headache, extreme fatigue, dry cough, sore throat, runny nose. Unlike common cold, flu symptoms are sudden and severe.")
                .prevention("Annual influenza vaccine (recommended for high-risk groups). Frequent handwashing. Avoid touching face. Cover cough and sneeze. Stay home when sick. Avoid crowded places during peak flu season.")
                .treatment("Antiviral (Oseltamivir/Zanamivir) within 48 hours of symptoms for high-risk patients. Paracetamol for fever. Rest and fluids. Available at Government hospitals. Call 104.")
                .contagious(true)
                .affectedAgeGroup("Children under 5, adults over 65, immunocompromised patients")
                .nameHi("फ्लू / इन्फ्लुएंजा")
                .symptomsHi("अचानक बुखार, तेज़ शरीर दर्द, सिरदर्द, थकान, खांसी, गले में दर्द।")
                .preventionHi("सालाना Flu टीका लगवाएं। हाथ साफ रखें। बीमार होने पर घर पर रहें।")
                .nameOr("ଫ୍ଲୁ / ଇନ୍‌ଫ୍ଲୁଏଞ୍ଜା")
                .symptomsOr("ଅଚାନକ ଜ୍ୱର, ଶରୀର ଯନ୍ତ୍ରଣା, ମୁଣ୍ଡ ବ୍ୟଥା, ଥକ୍କାଣ, ଖାଂସି।")
                .preventionOr("ବାର୍ଷିକ Flu ଟୀକା ନିଅନ୍ତୁ। ହାତ ସଫା ରଖନ୍ତୁ।")
                .build(),

            // ── JAPANESE ENCEPHALITIS (Odisha-specific) ───────────────────────────
            DiseaseInfo.builder()
                .name("Japanese Encephalitis")
                .icdCode("A83.0")
                .category("Vector-Borne Disease")
                .description("Japanese Encephalitis (JE) is a mosquito-borne viral brain infection. Odisha (especially districts like Malkangiri, Koraput, Gajapati) is among the most endemic states in India.")
                .symptoms("Sudden high fever, headache, stiff neck, vomiting, confusion, seizures, coma. Children may show unusual behaviour or jerky movements.")
                .prevention("JE vaccine (SA-14-14-2 / JENVAC) under Universal Immunization Programme. Free at Government PHCs in endemic districts. Eliminate mosquito breeding grounds. Sleep under bed nets.")
                .treatment("No specific antiviral treatment. Supportive care — manage fever, seizures. Immediate hospitalisation required. Call 108 for ambulance. VRDL testing available at RMRC Bhubaneswar.")
                .contagious(false)
                .affectedAgeGroup("Children under 15 years most at risk")
                .nameHi("जापानी एनसेफेलाइटिस (दिमागी बुखार)")
                .symptomsHi("तेज बुखार, गर्दन अकड़ना, उल्टी, दौरे पड़ना, बेहोशी।")
                .preventionHi("JE टीका लगवाएं (Government PHC में मुफ्त)। मच्छरदानी लगाएं।")
                .nameOr("ଜାପାନୀ ଏନ୍‌ସେଫାଲାଇଟିସ")
                .symptomsOr("ଜ୍ୱର, ବେକ ଟାଣ, ବାନ୍ତି, ଛାରି, ବେହୋସ। ଶିଶୁଙ୍କ ଅସ୍ୱାଭାବିକ ଆଚରଣ।")
                .preventionOr("JE ଟୀକା ନିଅନ୍ତୁ (PHCରେ ମାଗଣା)। ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ।")
                .build(),

            // ── DIARRHOEA ─────────────────────────────────────────────────────────
            DiseaseInfo.builder()
                .name("Diarrhoea")
                .icdCode("A09")
                .category("Water-Borne Disease")
                .description("Acute diarrhoeal disease is a leading cause of under-5 mortality in India. Highly prevalent in Odisha during monsoon floods due to contaminated water sources.")
                .symptoms("Loose, watery stools 3 or more times a day, abdominal cramps, nausea, vomiting, fever, dehydration signs (dry mouth, sunken eyes, reduced urination, listlessness).")
                .prevention("WASH — Water, Sanitation, and Hygiene. Drink only boiled/safe water. Wash hands with soap before eating and after defecation. Use toilets (Swachh Bharat). Breastfeed infants. Safe food storage.")
                .treatment("Immediate ORS (Oral Rehydration Solution) — 1 litre boiled water + 6 tsp sugar + 1 tsp salt. Continue breastfeeding infants. Zinc tablets for children under 5. Seek medical help if diarrhoea persists > 3 days or blood in stool. FREE ORS packets at all PHCs.")
                .contagious(true)
                .affectedAgeGroup("Children under 5 years most severely affected")
                .nameHi("दस्त / अतिसार")
                .symptomsHi("पतले दस्त (दिन में 3+ बार), पेट में दर्द, उल्टी, निर्जलीकरण।")
                .preventionHi("हाथ साफ रखें। उबला पानी पिएं। खुले में शौच न करें। ORS पिएं।")
                .nameOr("ଝାଡ଼ା / ଅତିସାର")
                .symptomsOr("ଦିନରେ 3+ ଥର ଝାଡ଼ା, ଉଦର ଯନ୍ତ୍ରଣା, ବାନ୍ତି, ନିର୍ଜଳ।")
                .preventionOr("ହାତ ଧୁଅନ୍ତୁ। ଫୁଟା ପାଣି ପିଅନ୍ତୁ। ORS ପିଅନ୍ତୁ।")
                .build()
        );

        diseaseInfoRepository.saveAll(diseases);
        log.info("✅ Seeded {} disease records from government health database.", diseases.size());
    }

    // ─── Vaccination Schedule (NHM Universal Immunization Programme) ──────────

    @Transactional
    public void seedVaccineData() {
        if (vaccineScheduleRepository.count() > 0) {
            log.info("✅ Vaccine database already seeded ({} records). Skipping.", vaccineScheduleRepository.count());
            return;
        }

        log.info("💉 Seeding NHM Universal Immunization Programme vaccine schedules...");

        List<VaccineSchedule> vaccines = List.of(

            VaccineSchedule.builder()
                .vaccineName("OPV (Oral Polio Vaccine) - Birth Dose")
                .disease("Poliomyelitis")
                .targetAge("At Birth")
                .doseSchedule("Birth dose at hospital/delivery point")
                .numberOfDoses(5)
                .administrationRoute("Oral (2 drops)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government hospitals, PHCs, Sub-centres")
                .vaccineNameHi("ओपीवी - जन्म खुराक")
                .vaccineNameOr("ଓପିଭି - ଜନ୍ମ ଡୋଜ")
                .descriptionHi("पोलियो से बचाव के लिए जन्म के समय 2 बूँद ओपीवी दी जाती है।")
                .descriptionOr("ପୋଲିଓ ରୋଗରୁ ରକ୍ଷା ପାଇଁ ଜନ୍ମ ସମୟରେ 2 ବୁନ୍ଦ ଓପିଭି ଦିଆ ଯାଏ।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("BCG (Bacillus Calmette-Guerin)")
                .disease("Tuberculosis")
                .targetAge("At Birth (within 24 hours)")
                .doseSchedule("Single dose at birth")
                .numberOfDoses(1)
                .administrationRoute("Intradermal injection (left upper arm)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government delivery points and PHCs")
                .vaccineNameHi("बीसीजी (BCG)")
                .vaccineNameOr("ବିସিଜି (BCG)")
                .descriptionHi("टीबी (तपेदिक) से बचाव के लिए जन्म के 24 घंटे के अंदर BCG टीका लगाया जाता है।")
                .descriptionOr("ଯକ୍ଷ୍ମାରୁ ରକ୍ଷା ପାଇଁ ଜନ୍ମ ପରେ 24 ଘଣ୍ଟା ମଧ୍ୟରେ BCG ଟୀକା ଦିଆ ଯାଏ।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("Hepatitis B (Hep-B) - Birth Dose")
                .disease("Hepatitis B")
                .targetAge("At Birth (within 24 hours)")
                .doseSchedule("Birth dose — then at 6 weeks and 14 weeks with pentavalent")
                .numberOfDoses(3)
                .administrationRoute("Intramuscular injection (right thigh)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government hospitals and PHCs")
                .vaccineNameHi("हेपेटाइटिस बी")
                .vaccineNameOr("ହେପାଟାଇଟିସ ବି")
                .descriptionHi("जन्म के समय + 6 हफ्ते + 14 हफ्ते पर 3 खुराक। यकृत संक्रमण से सुरक्षा।")
                .descriptionOr("ଜନ୍ମ + 6 ସପ୍ତାହ + 14 ସପ୍ତାହରେ 3 ଡୋଜ। ଯକୃତ ସଂକ୍ରମଣରୁ ସୁରକ୍ଷା।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("Pentavalent Vaccine (DPT + Hep-B + Hib)")
                .disease("Diphtheria, Pertussis, Tetanus, Hepatitis B, Haemophilus influenzae type b")
                .targetAge("6 Weeks, 10 Weeks, 14 Weeks")
                .doseSchedule("3 doses at 6, 10, and 14 weeks of age")
                .numberOfDoses(3)
                .administrationRoute("Intramuscular injection (anterior thigh)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government PHCs and Sub-centres")
                .vaccineNameHi("पंचावलेंट वैक्सीन (DPT+HepB+Hib)")
                .vaccineNameOr("ପେଣ୍ଟାଭ୍ୟାଲେଣ୍ଟ ଟୀକା")
                .descriptionHi("5 खतरनाक बीमारियों से एक साथ सुरक्षा। 6, 10 और 14 हफ्ते पर 3 खुराक।")
                .descriptionOr("5 ରୋଗ ବିରୁଦ୍ଧ ଗୋଟିଏ ଟୀକା। 6, 10, 14 ସପ୍ତାହରେ 3 ଡୋଜ।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("Rotavirus Vaccine (RVV)")
                .disease("Rotavirus Diarrhoea")
                .targetAge("6 Weeks, 10 Weeks, 14 Weeks")
                .doseSchedule("3 doses at 6, 10, and 14 weeks")
                .numberOfDoses(3)
                .administrationRoute("Oral (5 drops)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government PHCs. Now included in UIP for all states.")
                .vaccineNameHi("रोटावायरस वैक्सीन")
                .vaccineNameOr("ରୋଟାଭାଇରସ ଟୀକା")
                .descriptionHi("बच्चों में दस्त के सबसे बड़े कारण रोटावायरस से सुरक्षा। मुँह से 5 बूँद दी जाती है।")
                .descriptionOr("ଶିଶୁ ଝାଡ଼ାର ମୁଖ୍ୟ କାରଣ ରୋଟାଭାଇରସ ବିରୁଦ୍ଧ ସୁରକ୍ଷା। 5 ବୁନ୍ଦ ଖୁଆଯାଏ।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("MR Vaccine (Measles-Rubella)")
                .disease("Measles & Rubella")
                .targetAge("9-12 Months, 16-24 Months")
                .doseSchedule("First dose at 9-12 months; Second dose at 16-24 months")
                .numberOfDoses(2)
                .administrationRoute("Subcutaneous injection (right upper arm)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government PHCs, Anganwadi centres, and immunization sessions")
                .vaccineNameHi("MR वैक्सीन (खसरा-रूबेला)")
                .vaccineNameOr("MR ଟୀକା (ହାମ-ରୁବେଲା)")
                .descriptionHi("खसरा और रूबेला से बचाव। 9 महीने और 16-24 महीने पर 2 खुराक।")
                .descriptionOr("ହାମ ଓ ରୁବେଲାରୁ ସୁରକ୍ଷା। 9 ମାସ ଓ 16-24 ମାସରେ 2 ଡୋଜ।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("DPT Booster")
                .disease("Diphtheria, Pertussis, Tetanus")
                .targetAge("16-24 Months, 5-6 Years")
                .doseSchedule("First booster at 16-24 months; Second booster at 5-6 years")
                .numberOfDoses(2)
                .administrationRoute("Intramuscular injection")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government health facilities")
                .vaccineNameHi("DPT बूस्टर")
                .vaccineNameOr("DPT ବୁଷ୍ଟର")
                .descriptionHi("डिप्थीरिया, काली खांसी और टेटनस के खिलाफ प्रतिरोधक क्षमता बढ़ाने के लिए बूस्टर खुराक।")
                .descriptionOr("ଡିଫ୍‌ଥେରିଆ, ହୁପିଂ କଫ ଓ ଟিটାନସ ବିରୁଦ୍ଧ ରୋଗ ପ୍ରତିରୋଧ ବଢ଼ାଇବା।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("JE Vaccine (Japanese Encephalitis)")
                .disease("Japanese Encephalitis")
                .targetAge("9-12 Months (endemic districts only)")
                .doseSchedule("Single dose at 9-12 months in JE-endemic districts")
                .numberOfDoses(1)
                .administrationRoute("Subcutaneous injection")
                .mandatoryUnderNHM(true)
                .availability("FREE at Government PHCs in endemic districts: Malkangiri, Koraput, Gajapati, Rayagada, Nabarangpur, Balangir, Sambalpur, Kendujhar and others in Odisha")
                .vaccineNameHi("JE वैक्सीन (जापानी एनसेफेलाइटिस)")
                .vaccineNameOr("JE ଟୀକା")
                .descriptionHi("मस्तिष्क ज्वर (दिमागी बुखार) से बचाव के लिए ओडिशा के प्रभावित जिलों में मुफ्त टीका।")
                .descriptionOr("ଓଡ଼ିଶାର ଜୁଡ଼ିତ ଜିଲ୍ଲାରେ ଦିମାଗୁ ଜ୍ୱର ଠାରୁ ରକ୍ଷା ପାଇଁ ମାଗଣା JE ଟୀକା।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("Td Vaccine (Tetanus-Diphtheria) for Pregnant Women")
                .disease("Tetanus & Diphtheria in newborns and mothers")
                .targetAge("Pregnant Women — Early and late pregnancy")
                .doseSchedule("First dose at 12-16 weeks of pregnancy; Second dose at least 4 weeks after first dose")
                .numberOfDoses(2)
                .administrationRoute("Intramuscular injection (upper arm)")
                .mandatoryUnderNHM(true)
                .availability("FREE at all Government health facilities, PHCs, and through ASHA/ANM home visits")
                .vaccineNameHi("Td वैक्सीन (गर्भवती महिलाओं के लिए)")
                .vaccineNameOr("Td ଟୀକା (ଗର୍ଭବତୀ ମହିଳାଙ୍କ ପାଇଁ)")
                .descriptionHi("गर्भावस्था में 2 खुराक — माँ और नवजात शिशु को टेटनस और डिप्थीरिया से बचाती है।")
                .descriptionOr("ଗର୍ଭ ସମୟରେ 2 ଡୋଜ — ମା ଓ ଶିଶୁଙ୍କୁ ଟিটାନସ ଓ ଡିଫ୍‌ଥେରିଆ ଠାରୁ ରକ୍ଷା।")
                .build(),

            VaccineSchedule.builder()
                .vaccineName("COVID-19 Vaccine (Covishield / Covaxin / Corbevax)")
                .disease("COVID-19")
                .targetAge("All individuals 18 years and above")
                .doseSchedule("Primary series: 2 doses; Precaution dose for eligible groups")
                .numberOfDoses(3)
                .administrationRoute("Intramuscular injection (deltoid muscle)")
                .mandatoryUnderNHM(false)
                .availability("Available at Government hospitals and PHCs. Register on CoWIN portal: cowin.gov.in")
                .vaccineNameHi("COVID-19 वैक्सीन")
                .vaccineNameOr("COVID-19 ଟୀକା")
                .descriptionHi("18+ वर्ष के सभी लोगों के लिए COVID-19 वैक्सीन। CoWIN पोर्टल पर पंजीकरण करें।")
                .descriptionOr("18+ ବର୍ଷ ସମସ୍ତଙ୍କ ପାଇଁ COVID-19 ଟୀକା। CoWIN ପୋର୍ଟାଲରେ ପଞ୍ଜୀକରଣ କରନ୍ତୁ।")
                .build()
        );

        vaccineScheduleRepository.saveAll(vaccines);
        log.info("✅ Seeded {} vaccine schedules from NHM Universal Immunization Programme.", vaccines.size());
    }
}
