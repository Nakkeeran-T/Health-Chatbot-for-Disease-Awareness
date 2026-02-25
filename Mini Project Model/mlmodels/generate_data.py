"""
Generate synthetic training datasets for all 3 ML models.
Run: python generate_data.py
"""
import csv
import os

os.makedirs("data", exist_ok=True)

# ─────────────────────────────────────────────────────────────────────────────
# 1. INTENT CLASSIFICATION DATA  (text, intent)
# ─────────────────────────────────────────────────────────────────────────────
intent_data = []

samples = {
    "greeting": [
        "hello", "hi there", "hey", "namaste", "namaskar", "good morning",
        "start", "help me", "how are you", "ہیلو", "ହେଲୋ", "नमस्ते",
        "हैलो", "ନମସ୍କାର", "hi bot", "hello health assistant",
        "good afternoon", "good evening", "hey there", "hola",
    ],
    "disease_symptoms": [
        "what are the symptoms", "symptoms of fever", "I feel sick",
        "I have body pain", "feeling unwell", "what are signs of illness",
        "I am ill", "what does fever feel like", "rash on skin",
        "लक्षण क्या हैं", "बीमारी के लक्षण", "लकक्षण", "ଲକ୍ଷଣ",
        "बीमार हूं", "ଅସୁସ୍ଥ ଅଛି", "body ache and weakness",
        "what are symptoms", "signs of disease", "I have nausea",
        "I have vomiting and diarrhea", "swollen lymph nodes",
    ],
    "disease_prevention": [
        "how to prevent disease", "prevent infection", "how to stay safe",
        "protection from disease", "precautions to take", "how to avoid getting sick",
        "रोकथाम कैसे करें", "बचाव के उपाय", "सुरक्षा", "ପ୍ରତିରୋଧ",
        "how to protect myself", "disease prevention tips", "hygiene tips",
        "stay healthy tips", "how to avoid infection", "safety measures",
        "what precautions should I take", "how to prevent spreading",
        "preventive measures", "keep disease away",
    ],
    "vaccine": [
        "vaccine schedule", "vaccination", "when to vaccinate", "which vaccine",
        "immunization", "dose of vaccine", "free vaccine", "jab", "shot",
        "टीका", "वैक्सीन", "टीकाकरण", "ଟୀକା", "ଟୀକାକରଣ",
        "when should I get vaccinated", "how many doses", "vaccine for children",
        "get vaccinated", "covid vaccine", "polio vaccine", "bcg vaccine",
        "hepatitis vaccine", "which vaccines are available", "immunization schedule",
    ],
    "outbreak_alert": [
        "outbreak alert", "epidemic", "cases reported", "disease spread",
        "health emergency", "latest news", "new cases", "alert near me",
        "प्रकोप", "महामारी", "अलर्ट", "ପ୍ରାଦୁର୍ଭାବ", "ଆଲର୍ଟ",
        "any outbreaks", "current epidemic", "is there an outbreak",
        "health crisis", "disease spreading in area", "recent cases",
        "disease alert", "what outbreaks are happening",
    ],
    "malaria": [
        "malaria symptoms", "malaria treatment", "malaria prevention",
        "mosquito disease", "plasmodium", "fever after mosquito bite",
        "मलेरिया के लक्षण", "मलेरिया से बचाव", "ম্যালেরিয়া",
        "ମ୍ୟାଲେରିଆ", "मलेरिया", "malaria cure", "anti malaria",
        "do I have malaria", "malaria medicine", "malaria tablets",
        "malaria in odisha", "malaria drugs", "how to treat malaria",
        "malaria fever chills", "mosquito bite fever",
    ],
    "dengue": [
        "dengue fever", "dengue symptoms", "dengue prevention",
        "aedes mosquito", "dengue rash", "platelet count dengue",
        "डेंगू", "ডেঙ্গু", "ଡେଙ୍ଗୁ", "dengue treatment",
        "dengue cure", "dengue outbreak", "dengue hemorrhagic fever",
        "dengue headache", "dengue bone pain", "breakbone fever",
        "dengue spreading", "dengue cases", "how to prevent dengue",
        "dengue vaccine", "dengue shock syndrome",
    ],
    "tuberculosis": [
        "tuberculosis symptoms", "tb treatment", "tb symptoms",
        "lung disease", "persistent cough", "night sweats tb",
        "तपेदिक", "टीबी", "क्षय रोग", "ଯକ୍ଷ୍ମା", "ଟିବି",
        "tb cure", "tb medicine", "is tb curable", "tb vaccination",
        "bcg vaccine tb", "sputum test tb", "tb infection",
        "drug resistant tb", "pulmonary tb", "dots treatment",
        "tb in lungs", "cough blood tb",
    ],
    "cholera": [
        "cholera symptoms", "cholera treatment", "rice water stool",
        "dehydration", "watery diarrhea cholera", "ors solution",
        "हैजा", "਼कॉलरा", "ଭ‍ₓ‍", "cholera cure",
        " କଲେରା", "cholera prevention", "cholera water", "cholera outbreak",
        "oral rehydration", "extreme diarrhea", "cholera bacteria",
        "vibrio cholerae", "cholera vaccination", "contaminated water disease",
        "cholera cramps", "severe dehydration",
    ],
    "covid": [
        "covid symptoms", "coronavirus", "covid-19", "covid test",
        "covid vaccine", "covid prevention", "corona", "flu like symptoms",
        "कोविड", "कोरोना", " କୋଭିଡ", "covid positive",
        "loss of smell", "loss of taste", "covid isolation",
        "covid quarantine", "covid treatment", "covid helpline",
        "covid mask", "covid sanitizer", "covid social distancing",
        "covid19 symptoms", "covid booster",
    ],
    "typhoid": [
        "typhoid fever", "typhoid symptoms", "typhoid treatment",
        "enteric fever", "typhoid prevention", "typhoid vaccine",
        "टाइफाइड", "ਟਾਈਫ਼ਾਈਡ", "ଟାଇଫଏଡ",
        "salmonella typhi", "typhoid food", "typhoid water",
        "typhoid test", "typhoid antibiotics", "typhoid duration",
        "prolonged fever typhoid", "widal test", "typhoid carrier",
        "typhoid rose spots", "typhoid abdominal pain",
    ],
    "child_health": [
        "child health", "baby vaccination", "infant fever", "newborn care",
        "kids health", "child development", "toddler sick",
        "बच्चे की सेहत", "शिशु", "ଶିଶୁ", "child immunization",
        "baby health", "child fever treatment", "when to vaccinate baby",
        "child nutrition", "child diarrhea", "baby symptoms",
        "child medicine dose", "RBSK programme", "anganwadi vaccination",
        "kids vaccine schedule", "child health tips",
    ],
    "emergency": [
        "emergency number", "ambulance", "hospital near me", "doctor urgent",
        "health helpline", "critical condition", "call for help",
        "आपातकाल", "अस्पताल", "ڈاکٹر", "ଜରୁରୀ",
        "medical emergency", "emergency contact", "108", "104",
        "urgent medical help", "nearest hospital", "health crisis",
        "accident hospital", "emergency room", "first aid",
        "emergency india", "odisha emergency",
    ],
    "bye": [
        "bye", "goodbye", "thank you", "thanks", "ok done", "exit",
        "I'm done", "quit", "that's all", "see you",
        "धन्यवाद", "अलविदा", "ありがとう", "ଧନ୍ୟବାଦ",
        "thanks for help", "thank you so much", "great help",
        "take care", "good bye", "farewell",
    ],
    "general_query": [
        "what is health", "tell me about diseases", "health information",
        "general health tips", "diet advice", "nutrition tips",
        "स्वास्थ्य जानकारी", "ସ୍ୱାସ୍ଥ୍ୟ",
        "how to be healthy", "healthy lifestyle", "fitness tips",
        "medical advice", "health awareness", "public health",
        "what is WHO", "india health scheme", "ayushman bharat",
        "what is NHM", "PMJAY scheme", "health insurance",
    ],
}

for intent, texts in samples.items():
    for text in texts:
        intent_data.append({"text": text, "intent": intent})

with open("data/intent_training_data.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["text", "intent"])
    writer.writeheader()
    writer.writerows(intent_data)

print(f"✅ Intent data: {len(intent_data)} samples across {len(samples)} intents")

# ─────────────────────────────────────────────────────────────────────────────
# 2. DISEASE SYMPTOM PREDICTION DATA
#    Features: 25 binary symptom columns, Label: disease
# ─────────────────────────────────────────────────────────────────────────────
symptoms_list = [
    "fever", "cough", "headache", "chills", "nausea", "vomiting",
    "diarrhea", "fatigue", "body_pain", "rash", "loss_of_taste",
    "loss_of_smell", "night_sweats", "weight_loss", "chest_pain",
    "shortness_of_breath", "swollen_lymph", "eye_pain", "dehydration",
    "abdominal_pain", "bleeding", "jaundice", "sore_throat",
    "muscle_cramps", "blood_in_sputum"
]

disease_profiles = {
    "Malaria": {
        "fever": 1, "chills": 1, "headache": 1, "nausea": 1, "vomiting": 1,
        "fatigue": 1, "body_pain": 1, "night_sweats": 1, "dehydration": 1,
        "cough": 0, "diarrhea": 0, "rash": 0, "loss_of_taste": 0,
        "loss_of_smell": 0, "weight_loss": 0, "chest_pain": 0,
        "shortness_of_breath": 0, "swollen_lymph": 0, "eye_pain": 0,
        "abdominal_pain": 0, "bleeding": 0, "jaundice": 0,
        "sore_throat": 0, "muscle_cramps": 1, "blood_in_sputum": 0,
    },
    "Dengue": {
        "fever": 1, "headache": 1, "body_pain": 1, "eye_pain": 1, "rash": 1,
        "fatigue": 1, "nausea": 1, "vomiting": 1, "bleeding": 1,
        "muscle_cramps": 1, "chills": 0, "cough": 0, "diarrhea": 0,
        "loss_of_taste": 0, "loss_of_smell": 0, "night_sweats": 0,
        "weight_loss": 0, "chest_pain": 0, "shortness_of_breath": 0,
        "swollen_lymph": 0, "dehydration": 1, "abdominal_pain": 0,
        "jaundice": 0, "sore_throat": 0, "blood_in_sputum": 0,
    },
    "Tuberculosis": {
        "cough": 1, "blood_in_sputum": 1, "fever": 1, "night_sweats": 1,
        "weight_loss": 1, "chest_pain": 1, "fatigue": 1,
        "shortness_of_breath": 1, "chills": 0, "headache": 0, "nausea": 0,
        "vomiting": 0, "diarrhea": 0, "body_pain": 0, "rash": 0,
        "loss_of_taste": 0, "loss_of_smell": 0, "swollen_lymph": 1,
        "eye_pain": 0, "dehydration": 0, "abdominal_pain": 0,
        "bleeding": 0, "jaundice": 0, "sore_throat": 0, "muscle_cramps": 0,
    },
    "Cholera": {
        "diarrhea": 1, "vomiting": 1, "dehydration": 1, "muscle_cramps": 1,
        "nausea": 1, "fever": 0, "headache": 0, "chills": 0, "fatigue": 1,
        "body_pain": 0, "rash": 0, "loss_of_taste": 0, "loss_of_smell": 0,
        "night_sweats": 0, "weight_loss": 0, "chest_pain": 0,
        "shortness_of_breath": 0, "swollen_lymph": 0, "eye_pain": 0,
        "abdominal_pain": 1, "bleeding": 0, "jaundice": 0,
        "sore_throat": 0, "cough": 0, "blood_in_sputum": 0,
    },
    "COVID-19": {
        "fever": 1, "cough": 1, "shortness_of_breath": 1, "loss_of_taste": 1,
        "loss_of_smell": 1, "fatigue": 1, "body_pain": 1, "headache": 1,
        "sore_throat": 1, "chills": 1, "nausea": 0, "vomiting": 0,
        "diarrhea": 0, "rash": 0, "night_sweats": 0, "weight_loss": 0,
        "chest_pain": 1, "swollen_lymph": 0, "eye_pain": 0,
        "dehydration": 0, "abdominal_pain": 0, "bleeding": 0,
        "jaundice": 0, "muscle_cramps": 0, "blood_in_sputum": 0,
    },
    "Typhoid": {
        "fever": 1, "headache": 1, "abdominal_pain": 1, "fatigue": 1,
        "diarrhea": 1, "nausea": 1, "vomiting": 1, "weight_loss": 1,
        "rash": 1, "chills": 0, "cough": 0, "body_pain": 0,
        "night_sweats": 0, "chest_pain": 0, "loss_of_taste": 0,
        "loss_of_smell": 0, "shortness_of_breath": 0, "swollen_lymph": 0,
        "eye_pain": 0, "dehydration": 1, "bleeding": 0, "jaundice": 0,
        "sore_throat": 0, "muscle_cramps": 0, "blood_in_sputum": 0,
    },
    "Hepatitis": {
        "jaundice": 1, "fatigue": 1, "abdominal_pain": 1, "nausea": 1,
        "vomiting": 1, "fever": 1, "weight_loss": 1, "cough": 0,
        "headache": 0, "chills": 0, "diarrhea": 0, "body_pain": 0,
        "rash": 0, "loss_of_taste": 0, "loss_of_smell": 0,
        "night_sweats": 0, "chest_pain": 0, "shortness_of_breath": 0,
        "swollen_lymph": 0, "eye_pain": 0, "dehydration": 0,
        "bleeding": 0, "sore_throat": 0, "muscle_cramps": 0,
        "blood_in_sputum": 0,
    },
    "Influenza": {
        "fever": 1, "cough": 1, "body_pain": 1, "fatigue": 1, "headache": 1,
        "chills": 1, "sore_throat": 1, "nausea": 1, "vomiting": 1,
        "loss_of_taste": 0, "loss_of_smell": 0, "diarrhea": 0, "rash": 0,
        "night_sweats": 0, "weight_loss": 0, "chest_pain": 0,
        "shortness_of_breath": 0, "swollen_lymph": 0, "eye_pain": 0,
        "dehydration": 0, "abdominal_pain": 0, "bleeding": 0,
        "jaundice": 0, "muscle_cramps": 0, "blood_in_sputum": 0,
    },
    "Measles": {
        "fever": 1, "rash": 1, "cough": 1, "eye_pain": 1, "fatigue": 1,
        "sore_throat": 1, "headache": 1, "chills": 0, "nausea": 0,
        "vomiting": 0, "diarrhea": 0, "body_pain": 0, "loss_of_taste": 0,
        "loss_of_smell": 0, "night_sweats": 0, "weight_loss": 0,
        "chest_pain": 0, "shortness_of_breath": 0, "swollen_lymph": 0,
        "dehydration": 0, "abdominal_pain": 0, "bleeding": 0,
        "jaundice": 0, "muscle_cramps": 0, "blood_in_sputum": 0,
    },
    "Common Cold": {
        "cough": 1, "sore_throat": 1, "headache": 1, "fatigue": 1,
        "fever": 0, "chills": 0, "body_pain": 0, "rash": 0,
        "nausea": 0, "vomiting": 0, "diarrhea": 0, "loss_of_taste": 0,
        "loss_of_smell": 0, "night_sweats": 0, "weight_loss": 0,
        "chest_pain": 0, "shortness_of_breath": 0, "swollen_lymph": 0,
        "eye_pain": 0, "dehydration": 0, "abdominal_pain": 0,
        "bleeding": 0, "jaundice": 0, "muscle_cramps": 0,
        "blood_in_sputum": 0,
    },
}

# Generate multiple noisy variations per disease
import random
random.seed(42)

disease_rows = []
for disease, profile in disease_profiles.items():
    for _ in range(60):  # 60 variations per disease = 600 total
        row = {}
        for sym in symptoms_list:
            base = profile[sym]
            # add noise: 10% chance of flip
            row[sym] = base if random.random() > 0.10 else 1 - base
        row["disease"] = disease
        disease_rows.append(row)

fieldnames = symptoms_list + ["disease"]
with open("data/disease_symptom_data.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(disease_rows)

print(f"✅ Disease data: {len(disease_rows)} samples across {len(disease_profiles)} diseases")

# ─────────────────────────────────────────────────────────────────────────────
# 3. LANGUAGE DETECTION DATA  (text, language)
# ─────────────────────────────────────────────────────────────────────────────
lang_data = []

en_texts = [
    "I have fever and headache", "what are the symptoms of malaria",
    "vaccine schedule for children", "how to prevent dengue",
    "outbreak alert in my area", "emergency ambulance number",
    "tuberculosis treatment", "cholera symptoms and prevention",
    "covid vaccination center", "typhoid fever treatment",
    "mosquito bite prevention", "health helpline number",
    "disease symptoms check", "how to stay healthy",
    "boil water to prevent cholera", "wear masks in public",
    "wash hands frequently", "get vaccinated now",
    "free treatment at government hospital", "outbreak near me",
    "child vaccination program", "anganwadi health services",
    "national health mission scheme", "diarrhea and vomiting treatment",
    "ors solution for dehydration",
]

hi_texts = [
    "मुझे बुखार है", "मलेरिया के लक्षण क्या हैं", "टीकाकरण कार्यक्रम",
    "डेंगू से बचाव कैसे करें", "प्रकोप अलर्ट", "एम्बुलेंस नंबर",
    "तपेदिक का इलाज", "हैजा के लक्षण", "कोविड टीका",
    "टाइफाइड बुखार", "मच्छर से बचाव", "स्वास्थ्य हेल्पलाइन",
    "रोग के लक्षण", "स्वस्थ कैसे रहें", "पानी उबालकर पिएं",
    "मास्क पहनें", "हाथ धोएं", "टीका लगवाएं",
    "सरकारी अस्पताल में मुफ्त इलाज", "नजदीकी प्रकोप",
    "बच्चों का टीकाकरण", "आंगनवाड़ी स्वास्थ्य सेवा",
    "राष्ट्रीय स्वास्थ्य मिशन", "दस्त और उल्टी का इलाज",
    "ओआरएस घोल",
]

or_texts = [
    "ମୋତେ ଜ୍ୱର ଅଛି", "ମ୍ୟାଲେରିଆ ଲକ୍ଷଣ କ'ଣ", "ଟୀକାକରଣ କାର୍ଯ୍ୟକ୍ରମ",
    "ଡେଙ୍ଗୁ ଠାରୁ ରକ୍ଷା", "ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ", "ଆ୍ୟାମ୍ବୁଲାନ୍ସ ନଂ",
    "ଯକ୍ଷ୍ମା ଚିକିତ୍ସା", "କଲେରା ଲକ୍ଷଣ", "କୋଭିଡ ଟୀକା",
    "ଟାଇଫଏଡ ଜ୍ୱର", "ମଶା ଦୂରେଇ ରହନ୍ତୁ", "ସ୍ୱାସ୍ଥ୍ୟ ହେଲ୍ପଲାଇନ",
    "ରୋଗ ଲକ୍ଷଣ", "ସୁସ୍ଥ ରୁହନ୍ତୁ", "ଜଳ ଫୁଟାଇ ପିଅନ୍ତୁ",
    "ମାସ୍କ ପିନ୍ଧନ୍ତୁ", "ହାତ ଧୁଅନ୍ତୁ", "ଟୀକା ନିଅନ୍ତୁ",
    "ସରକାରୀ ଅସ୍ପତାଳ ମାଗଣା ଚିକିତ୍ସା", "ନିକଟ ପ୍ରାଦୁର୍ଭାବ",
    "ଶିଶୁ ଟୀକାକରଣ", "ଆଙ୍ଗନବାଡ଼ି ସ୍ୱାସ୍ଥ୍ୟ ସେବା",
    "ଜାତୀୟ ସ୍ୱାସ୍ଥ୍ୟ ମିଶନ", "ଝାଡ଼ା ଓ ବାନ୍ତି ଚିକିତ୍ସା",
    "ORS ଦ୍ରବଣ",
]

for t in en_texts:
    lang_data.append({"text": t, "language": "en"})
for t in hi_texts:
    lang_data.append({"text": t, "language": "hi"})
for t in or_texts:
    lang_data.append({"text": t, "language": "or"})

with open("data/language_detection_data.csv", "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["text", "language"])
    writer.writeheader()
    writer.writerows(lang_data)

print(f"✅ Language data: {len(lang_data)} samples (EN/HI/OR)")
print("\n🎉 All datasets generated in data/ directory!")
