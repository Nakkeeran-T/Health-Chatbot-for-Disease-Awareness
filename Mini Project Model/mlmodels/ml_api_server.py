"""
Flask REST API server for ML model inference.
Exposes endpoints for the Spring Boot backend and standalone chatbot demo.

Start : python ml_api_server.py
Port  : 5002
"""
import os
import random
from datetime import datetime
import sys

# Ensure stdout uses UTF-8 to prevent UnicodeEncodeError with emojis on Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

import re
import joblib
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS

import requests as http_requests

app = Flask(__name__)
CORS(app)

# ─── Load models ─────────────────────────────────────────────────────────────
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

intent_model   = None
disease_bundle = None
lang_model     = None
real_image_model = None
image_classes  = None

def load_models():
    global intent_model, disease_bundle, lang_model, real_image_model, image_classes

    intent_path  = os.path.join(MODEL_DIR, "intent_classifier.pkl")
    disease_path = os.path.join(MODEL_DIR, "disease_predictor.pkl")
    lang_path    = os.path.join(MODEL_DIR, "lang_detector.pkl")

    missing = []
    for p, name in [(intent_path, "intent_classifier"),
                    (disease_path, "disease_predictor"),
                    (lang_path, "lang_detector")]:
        if not os.path.exists(p):
            missing.append(name)

    if missing:
        print(f"❌ Missing model files: {missing}")
        print("   Run: python train_models.py  to train the models first.")
        sys.exit(1)

    print("📦 Loading ML models...")
    intent_model   = joblib.load(intent_path)
    disease_bundle = joblib.load(disease_path)
    lang_model     = joblib.load(lang_path)
    print("✅ All basic models loaded successfully!")

    # Attempt to load custom image model
    image_model_path = os.path.join(MODEL_DIR, "image_disease_model.h5")
    image_classes_path = os.path.join(MODEL_DIR, "image_classes.pkl")
    if os.path.exists(image_model_path) and os.path.exists(image_classes_path):
        try:
            from tensorflow.keras.models import load_model
            print("📦 Loading Custom Image Prediction Model...")
            real_image_model = load_model(image_model_path)
            image_classes = joblib.load(image_classes_path)
            print("✅ Custom Image Model loaded successfully!")
        except Exception as e:
            print(f"⚠️ Failed to load custom image model: {e}")
            real_image_model = None
            image_classes = None
    else:
        print("⚠️ Custom image model not found. Using Mock Image Predictor.")

# ─── Mock Image Model ────────────────────────────────────────────────────────
def predict_from_image(file_bytes):
    global real_image_model, image_classes
    
    # Use real model if trained and loaded successfully
    if real_image_model is not None and image_classes is not None:
        try:
            from tensorflow.keras.preprocessing.image import img_to_array
            from PIL import Image
            import io
            
            img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
            img = img.resize((224, 224))
            
            img_array = img_to_array(img) / 255.0
            img_array = np.expand_dims(img_array, axis=0)
            
            predictions = real_image_model.predict(img_array, verbose=0)[0]
            max_idx = np.argmax(predictions)
            
            pred_disease = image_classes[max_idx]
            confidence = float(predictions[max_idx])
            
            info = DISEASE_INFO.get(pred_disease, {})
            
            return {
                "disease": pred_disease.replace("_", " ").title(),
                "confidence": round(confidence, 4),
                "prevention": info.get("prevention", "Maintain a healthy routine. Protect your skin from extreme elements."),
                "treatment": info.get("treatment", "Consult a certified doctor or dermatologist for a proper diagnosis."),
                "helpline": info.get("helpline", "104")
            }
        except Exception as e:
            print(f"[Image Predict Error] Model inference failed: {e}")
            pass # Fall back to mock if inference crashed
            
    # Mock fallback
    diseases = ["Melanoma (Skin Cancer)", "Eczema", "Psoriasis", "Benign (Healthy)"]
    probs = [0.15, 0.35, 0.10, 0.40] # probabilities
    pred = np.random.choice(diseases, p=probs)
    return {
        "disease": pred + " (Mock Preview)",
        "confidence": round(random.uniform(0.75, 0.98), 4),
        "prevention": "Avoid excessive sun exposure. Keep skin moisturized. Maintain good hygiene.",
        "treatment": "Consult a dermatologist for proper diagnosis and medical treatment.",
        "helpline": "104"
    }


# ─── LLM Integration (Google Gemini via REST API) ─────────────────────────────
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "")
GEMINI_MODEL   = "gemini-1.5-flash"
GEMINI_URL     = (
    "https://generativelanguage.googleapis.com/v1beta/models/"
    "{model}:generateContent?key={key}"
)

_SYSTEM_PROMPT = """You are a helpful AI Health Assistant for a public health chatbot serving rural communities in Odisha, India. You have expertise in:
- Common diseases: Malaria, Dengue, Tuberculosis, Cholera, Typhoid, COVID-19, Hepatitis, Influenza, Measles
- Vaccination schedules under India's National Health Mission (NHM)
- Disease prevention, hygiene, and first aid
- Emergency health helpline numbers (104, 108, 1075)
- Government health schemes and free treatment facilities

Guidelines:
1. Always respond in the SAME LANGUAGE as the user's message (English / Hindi / Odia).
2. Be warm, empathetic, and conversational — never robotic or like a template.
3. Use simple language that rural communities can understand (avoid jargon).
4. Always recommend consulting a real doctor; you can assist but not diagnose.
5. When a disease is predicted, naturally weave in: symptoms recap, prevention, treatment, helpline.
6. Keep responses concise but informative (3-5 short paragraphs or bullet points).
7. Use relevant emojis (🌡️🦟💉🏥📞🛡️) to make responses visually engaging.
8. NEVER make up drug names, dosages, or specific medical advice beyond general health guidance.
9. If asked about non-health topics, politely redirect to health questions."""

def generate_llm_response(
    user_message: str,
    intent: str,
    language: str,
    symptoms_found: list,
    disease_prediction: dict | None,
) -> str | None:
    if not GEMINI_API_KEY:
        return None  # No API key → caller will use template fallback

    lang_name = {"en": "English", "hi": "Hindi", "or": "Odia"}.get(language, "English")

    context_lines = [
        f"User message: {user_message}",
        f"Detected language: {lang_name}",
        f"Detected intent: {intent}",
    ]
    if symptoms_found:
        context_lines.append(f"Symptoms mentioned by user: {', '.join(symptoms_found)}")
    if disease_prediction:
        d = disease_prediction
        context_lines.append(
            f"ML Disease Prediction: {d['disease']} "
            f"(confidence: {d['confidence'] * 100:.0f}%)\n"
            f"  Prevention: {d.get('prevention', 'Consult a healthcare professional.')}\n"
            f"  Treatment: {d.get('treatment', 'Visit nearest PHC.')}\n"
            f"  Helpline: {d.get('helpline', '104')}"
        )
        alts = [x for x in d.get("top_diseases", []) if x["disease"] != d["disease"]]
        if alts:
            context_lines.append(
                "Other possible diseases (lower probability): "
                + ", ".join(f"{x['disease']} ({x['probability']*100:.0f}%)" for x in alts)
            )

    context_block = "\n".join(context_lines)

    full_prompt = (
        f"{_SYSTEM_PROMPT}\n\n"
        f"--- ML Model Analysis (use as grounding facts) ---\n"
        f"{context_block}\n"
        f"--- End of ML Analysis ---\n\n"
        f"Now write a helpful, warm, natural response in {lang_name} to the user's message. "
        f"Use the ML analysis as factual grounding. Be conversational, not like a form."
    )

    payload = {
        "contents": [{"parts": [{"text": full_prompt}]}],
        "generationConfig": {
            "temperature":     0.75,
            "topK":            40,
            "topP":            0.95,
            "maxOutputTokens": 900,
        },
        "safetySettings": [
            {"category": "HARM_CATEGORY_DANGEROUS_CONTENT",  "threshold": "BLOCK_ONLY_HIGH"},
            {"category": "HARM_CATEGORY_HATE_SPEECH",        "threshold": "BLOCK_ONLY_HIGH"},
            {"category": "HARM_CATEGORY_HARASSMENT",         "threshold": "BLOCK_ONLY_HIGH"},
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",  "threshold": "BLOCK_ONLY_HIGH"},
        ],
    }

    try:
        url  = GEMINI_URL.format(model=GEMINI_MODEL, key=GEMINI_API_KEY)
        resp = http_requests.post(url, json=payload, timeout=15)
        if resp.status_code == 200:
            data = resp.json()
            candidates = data.get("candidates", [])
            if candidates:
                parts = candidates[0].get("content", {}).get("parts", [])
                if parts:
                    text = parts[0].get("text", "").strip()
                    if text:
                        return text
        else:
            print(f"[Gemini] API error {resp.status_code}: {resp.text[:300]}")
    except Exception as exc:
        print(f"[Gemini] Request failed: {exc}")

    return None

# ─── Static knowledge maps ────────────────────────────────────────────────────

SUGGESTIONS_MAP = {
    "greeting":           ["Symptoms of Malaria", "Vaccine Schedule", "Active Alerts", "Dengue Prevention"],
    "disease_symptoms":   ["How to prevent it?", "Vaccine available?", "Nearest hospital", "Emergency contacts"],
    "disease_prevention": ["Hand hygiene tips", "Safe water guide", "Vaccination info", "Outbreak alerts"],
    "vaccine":            ["Child vaccination", "COVID vaccine", "Typhoid vaccine", "When to vaccinate?"],
    "outbreak_alert":     ["Malaria update", "Dengue outbreak", "Stay safe tips", "Health guidelines"],
    "malaria":            ["Malaria prevention", "Malaria vaccine?", "Nearest PHC", "Emergency: 108"],
    "dengue":             ["Dengue prevention", "Platelet count low?", "Dengue vaccine?", "Emergency: 108"],
    "tuberculosis":       ["DOTS treatment info", "TB vaccine", "Free TB test", "TB helpline"],
    "cholera":            ["ORS solution how", "Safe water tips", "Cholera prevention", "Nearest hospital"],
    "covid":              ["COVID vaccine", "Isolation guide", "COVID helpline: 1075", "Testing centers"],
    "typhoid":            ["Typhoid vaccine", "Safe food tips", "Typhoid test", "Nearest hospital"],
    "child_health":       ["Child vaccine schedule", "RBSK programme", "Anganwadi center", "Emergency: 108"],
    "emergency":          ["Call 108", "Call 104", "Nearest hospital", "First aid tips"],
    "bye":                ["Disease symptoms", "Vaccination info", "Health alerts", "Prevention tips"],
    "general_query":      ["Disease symptoms", "Vaccination info", "Health alerts", "Prevention tips"],
}

DISEASE_INFO = {
    "Malaria": {
        "prevention": "Sleep under insecticide-treated nets. Use mosquito repellent. Eliminate standing water.",
        "treatment":  "Consult a doctor. Free antimalarial treatment at government hospitals.",
        "helpline":   "104 (Health Helpline)",
    },
    "Dengue": {
        "prevention": "Remove stagnant water from pots and coolers. Use mosquito nets during daytime.",
        "treatment":  "Rest, fluids, paracetamol. Seek medical care if platelets drop.",
        "helpline":   "104",
    },
    "Tuberculosis": {
        "prevention": "BCG vaccine for newborns. Good ventilation. Avoid close contact with TB patients.",
        "treatment":  "FREE DOTS therapy at government health centres. Complete the full 6-month course!",
        "helpline":   "1800-116-666 (Nikshay Helpline)",
    },
    "Cholera": {
        "prevention": "Drink boiled/treated water. Cover food. Wash hands before eating.",
        "treatment":  "ORS immediately. IV fluids if severe. Rush to hospital.",
        "helpline":   "104",
    },
    "COVID-19": {
        "prevention": "Wear mask. Maintain 6-feet distance. Wash hands. Get vaccinated.",
        "treatment":  "Isolate. Rest. Paracetamol for fever. Call 1075 if breathing difficulty.",
        "helpline":   "1075 (National) | 104 (Odisha)",
    },
    "Typhoid": {
        "prevention": "Safe drinking water. Proper food hygiene. Typhoid vaccine available.",
        "treatment":  "Antibiotics prescribed by doctor. Complete the full course.",
        "helpline":   "104",
    },
    "Hepatitis": {
        "prevention": "Hepatitis B vaccine. Avoid sharing needles. Safe blood transfusion.",
        "treatment":  "Antiviral medication. Consult a gastroenterologist.",
        "helpline":   "104",
    },
    "Influenza": {
        "prevention": "Annual flu vaccine. Hand hygiene. Avoid crowded places when sick.",
        "treatment":  "Rest, fluids, paracetamol. Antiviral drugs if severe.",
        "helpline":   "104",
    },
    "Measles": {
        "prevention": "MR (Measles-Rubella) vaccine at 9 months. Keep children vaccinated.",
        "treatment":  "Vitamin A supplementation. Rest. Consult doctor.",
        "helpline":   "104",
    },
    "Common Cold": {
        "prevention": "Hand hygiene. Avoid close contact with infected individuals.",
        "treatment":  "Rest, fluids, honey-ginger tea. OTC decongestants if needed.",
        "helpline":   "104",
    },
    # ─── Image Model Skin Diseases ──────────────────────────────────────────
    "1. Eczema 1677": {
        "prevention": "Keep skin moisturized. Avoid harsh soaps. Identify and avoid triggers (stress, allergens).",
        "treatment":  "Use topical corticosteroids, emollients, and antihistamines as prescribed by a dermatologist.",
        "helpline":   "104",
    },
    "2. Melanoma 15.75k": {
        "prevention": "Wear sunscreen (SPF 30+). Wear protective clothing. Avoid tanning beds. Regularly check for new or changing moles.",
        "treatment":  "URGENT: Requires surgical removal, and possibly radiation or immunotherapy. Consult an oncologist immediately.",
        "helpline":   "104",
    },
    "3. Atopic Dermatitis - 1.25k": {
        "prevention": "Identify triggers. Maintain skin barrier with frequent moisturizing. Use mild cleansers.",
        "treatment":  "Topical steroids, calcineurin inhibitors, and phototherapy in severe cases.",
        "helpline":   "104",
    },
    "4. Basal Cell Carcinoma (BCC) 3323": {
        "prevention": "Sun protection is key. Avoid midday sun. Wear hats and sunglasses.",
        "treatment":  "Surgery (Mohs surgery or excision). Early detection leads to 100% cure rate.",
        "helpline":   "104",
    },
    "5. Melanocytic Nevi (NV) - 7970": {
        "prevention": "Most moles (nevi) are normal. Protect from sun to prevent changes.",
        "treatment":  "Usually benign. Watch for 'ABCDE' changes. Biopsy if suspicious.",
        "helpline":   "104",
    },
    "6. Benign Keratosis-like Lesions (BKL) 2624": {
        "prevention": "Generally harmless. No specific prevention required.",
        "treatment":  "Can be removed for cosmetic reasons or if irritated (cryosurgery, curettage).",
        "helpline":   "104",
    },
    "7. Psoriasis pictures Lichen Planus and related diseases - 2k": {
        "prevention": "Manage stress. Avoid skin injuries. Moisturize frequently.",
        "treatment":  "Topical treatments, light therapy, or systemic medications (biologics).",
        "helpline":   "104",
    },
    "8. Seborrheic Keratoses and other Benign Tumors - 1.8k": {
        "prevention": "Non-cancerous growths common with age. No known prevention.",
        "treatment":  "No treatment needed unless they are itchy or inflamed.",
        "helpline":   "104",
    },
    "9. Tinea Ringworm Candidiasis and other Fungal Infections - 1.7k": {
        "prevention": "Keep skin dry and clean. Do not share towels or personal items.",
        "treatment":  "Topical or oral antifungal medications as prescribed.",
        "helpline":   "104",
    },
    "10. Warts Molluscum and other Viral Infections - 2103": {
        "prevention": "Avoid direct contact with warts. Do not share razors or towels.",
        "treatment":  "Salicylic acid, cryotherapy (freezing), or laser treatment.",
        "helpline":   "104",
    },
}

ALL_SYMPTOMS = [
    "fever", "cough", "headache", "chills", "nausea", "vomiting",
    "diarrhea", "fatigue", "body_pain", "rash", "loss_of_taste",
    "loss_of_smell", "night_sweats", "weight_loss", "chest_pain",
    "shortness_of_breath", "swollen_lymph", "eye_pain", "dehydration",
    "abdominal_pain", "bleeding", "jaundice", "sore_throat",
    "muscle_cramps", "blood_in_sputum",
]

SYMPTOM_TO_DATASET_COL = {
    "fever": "high_fever",
    "cough": "cough",
    "headache": "headache",
    "chills": "chills",
    "nausea": "nausea",
    "vomiting": "vomiting",
    "diarrhea": "diarrhoea",
    "fatigue": "fatigue",
    "body_pain": "muscle_pain",
    "rash": "skin_rash",
    "loss_of_taste": "loss_of_appetite",
    "loss_of_smell": "loss_of_smell",
    "night_sweats": "sweating",
    "weight_loss": "weight_loss",
    "chest_pain": "chest_pain",
    "shortness_of_breath": "breathlessness",
    "swollen_lymph": "swelled_lymph_nodes",
    "eye_pain": "pain_behind_the_eyes",
    "dehydration": "dehydration",
    "abdominal_pain": "abdominal_pain",
    "bleeding": "stomach_bleeding",
    "jaundice": "yellowish_skin",
    "sore_throat": "throat_irritation",
    "muscle_cramps": "cramps",
    "blood_in_sputum": "blood_in_sputum"
}

SYMPTOM_SYNONYMS = {
    "fever":               ["fever", "temperature", "high temp", "pyrexia", "बुखार", "ज्वर", "ଜ୍ୱର"],
    "cough":               ["cough", "coughing", "खांसी", "ଖାଂସି"],
    "headache":            ["headache", "head ache", "head pain", "migraine", "सिरदर्द", "ମୁଣ୍ଡ ବ୍ୟଥା"],
    "chills":              ["chills", "chill", "shivering", "shiver", "rigor", "ठंड", "ଥଣ୍ଡା"],
    "nausea":              ["nausea", "nauseated", "feel sick", "जी मिचलाना", "ବାନ୍ତି ଭାବ"],
    "vomiting":            ["vomiting", "vomit", "throwing up", "puke", "उल्टी", "बाँटना", "ବାନ୍ତି"],
    "diarrhea":            ["diarrhea", "diarrhoea", "loose stools", "watery stool", "दस्त", "ଝାଡ଼ା"],
    "fatigue":             ["fatigue", "tired", "weakness", "lethargy", "थकान", "कमजोरी", "ଥକ୍କାଣ"],
    "body_pain":           ["body pain", "body ache", "muscle pain", "myalgia", "शरीर दर्द", "ଶରୀର ଯନ୍ତ୍ରଣା"],
    "rash":                ["rash", "skin rash", "spots", "eruption", "चकत्ते", "ଚର୍ମ ଦାଗ"],
    "loss_of_taste":       ["loss of taste", "no taste", "taste loss", "सैवाद न आना", "ଖାଦ୍ୟ ସ୍ୱାଦ ନ ଲାଗିବା"],
    "loss_of_smell":       ["loss of smell", "no smell", "smell loss", "गंध न आना", "ଗନ୍ଧ ନ ଲାଗିବା"],
    "night_sweats":        ["night sweats", "sweating at night", "रात को पसीना", "ରାତ୍ରି ଘାମ"],
    "weight_loss":         ["weight loss", "losing weight", "वजन कम", "ଓଜନ ହ୍ରାସ"],
    "chest_pain":          ["chest pain", "chest ache", "सीने में दर्द", "ଛାତି ଯନ୍ତ୍ରଣା"],
    "shortness_of_breath": ["shortness of breath", "difficulty breathing", "breathlessness", "dyspnea",
                            "सांस लेने में तकलीफ", "ଶ୍ୱାସ ସମସ୍ୟା"],
    "swollen_lymph":       ["swollen lymph", "swollen glands", "lymph node", "सूजी हुई ग्रंथियां"],
    "eye_pain":            ["eye pain", "eye ache", "आँखों में दर्द", "ଆଖ ଯନ୍ତ୍ରଣା"],
    "dehydration":         ["dehydration", "dehydrated", "thirst", "निर्जलीकरण", "ନିର୍ଜଳ"],
    "abdominal_pain":      ["abdominal pain", "stomach pain", "belly pain", "पेट दर्द", "ଉଦର ଯନ୍ତ୍ରଣା"],
    "bleeding":            ["bleeding", "bleed", "hemorrhage", "रक्तस्राव", "ରକ୍ତସ୍ରାବ"],
    "jaundice":            ["jaundice", "yellow eyes", "yellow skin", "पीलिया", "ଜଣ୍ଡିସ"],
    "sore_throat":         ["sore throat", "throat pain", "गले में दर्द", "ଗଳ ଯନ୍ତ୍ରଣା"],
    "muscle_cramps":       ["muscle cramps", "cramps", "spasm", "मांसपेशियों में ऐंठन", "ଖଞ୍ଜ"],
    "blood_in_sputum":     ["blood in sputum", "coughing blood", "haemoptysis", "बलगम में खून", "ଥୁ ରକ୍ତ"],
}


import os
import random
import requests as http_requests
from datetime import datetime
from flask import jsonify, request

def _time_greeting(lang: str) -> str:
    hour = datetime.now().hour
    if lang == "hi":
        if hour < 12: return "सुप्रभात"
        elif hour < 17: return "शुभ दोपहर"
        else: return "शुभ संध्या"
    elif lang == "or":
        if hour < 12: return "ଶୁଭ ସକାଳ"
        elif hour < 17: return "ଶୁଭ ଅପରାହ୍ନ"
        else: return "ଶୁଭ ସନ୍ଧ୍ୟା"
    else:
        if hour < 12: return "Good morning"
        elif hour < 17: return "Good afternoon"
        else: return "Good evening"

# The RESPONSES dictionary from original file (abridged based on what I saw)
RESPONSES = {
    "malaria": {
        "en": "Malaria is transmitted by mosquitoes. Symptoms include fever and chills. Keep surroundings clean.",
        "hi": "मलेरिया मच्छरों द्वारा फैलता है। बुखार और ठंड लगना इसके लक्षण हैं। आसपास सफाई रखें।",
        "or": "ମଶା କାମୁଡିଲେ ମ୍ୟାଲେରିଆ ହୁଏ। ଜ୍ଵର ଓ ଥଣ୍ଡା ଏହାର ଲକ୍ଷଣ ଅଟେ। ଆଖପାଖ ସଫା ରଖନ୍ତୁ।"
    },
    "dengue": {
        "en": "Dengue is a mosquito-borne viral disease. Prevent mosquito bites and eliminate standing water.",
        "hi": "डेंगू मच्छरों से फैलने वाला रोग है। पानी जमा न होने दें।",
        "or": "ଡେଙ୍ଗୁ ମଶାମାନଙ୍କ ଦ୍ବାରା ବ୍ୟାପିଥାଏ। ପାଣି ଜମା ହେବାକୁ ଦିଅନ୍ତୁ ନାହିଁ।"
    },
    "general_query": {
        "en": "I can help with health information, disease prevention, and outbreak alerts. How can I assist you?",
        "hi": "मैं स्वास्थ्य जानकारी और बीमारी से बचाव में मदद कर सकता हूँ। मैं आपकी कैसे मदद करूँ?",
        "or": "ମୁଁ ସ୍ବାସ୍ଥ୍ୟ ସୂଚନା ଏବଂ ରୋଗ ନିରାକରଣରେ ସାହାଯ୍ୟ କରିପାରିବି। ମୁଁ ଆପଣଙ୍କୁ କିପରି ସାହାଯ୍ୟ କରିବି?"
    }
}

# Add logic for RESPONSES variants from the fix script.
RESPONSE_VARIANTS = {
    "disease_prevention": {
        "en": [
            "🩺 **Top Tips to Stay Healthy**\n\n✅ Wash hands with soap and water\n✅ Wear full clothes in mosquito areas\n✅ Keep children vaccinated\n✅ See a doctor if early symptoms appear",
        ],
        "hi": [
            ("🩺 **स्वस्थ रहने के बेहतरीन तरीके**\n\n"
             "✅ खाने से पहले और शौच के बाद हाथ धोएं\n"
             "✅ मच्छरों वाले इलाकों में पूरे कपड़े पहनें\n"
             "✅ बच्चों को नियमित टीका लगवाएं\n"
             "✅ बीमारी के शुरुआती लक्षण आने पर डॉक्टर से मिलें"),
        ],
        "or": [
            ("🛡️ **ରୋଗ ପ୍ରତିରୋଧ ଉପାୟ**\n\n"
             "1. 🧼 ନିୟମିତ ହାତ ଧୁଅନ୍ତୁ\n"
             "2. 💧 ବିଶୁଦ୍ଧ ଜଳ ପିଅନ୍ତୁ\n"
             "3. 🦟 ମଶାରୀ ବ୍ୟବହାର କରନ୍ତୁ\n"
             "4. 💉 ସମୟ ଅନୁଯାୟୀ ଟୀକା ନିଅନ୍ତୁ\n"
             "5. 🥗 ପୌଷ୍ଟିକ ଖାଦ୍ୟ ଖାଆନ୍ତୁ"),
        ],
    },
    "bye": {
        "en": [
            "😊 **Thank you for using the Health Chatbot!**\n\nStay healthy, stay safe! 🙏\n\nRemember: Prevention is always better than cure!",
            "👋 **Take care and stay well!** If you have more health questions anytime, I am here. 🙏",
            "🌟 **Goodbye and stay safe!** Wash hands, drink clean water, get vaccinated and stay strong! 💪",
        ],
        "hi": [
            "😊 **धन्यवाद!** स्वस्थ रहें, सुरक्षित रहें! 🙏",
            "👋 **अपना ख्याल रखें!** कभी भी स्वास्थ्य संबंधी सवाल हो, मुझसे पूछें। 🙏",
        ],
        "or": [
            "😊 **ଧନ୍ୟବାଦ!** ସୁସ୍ଥ ଥାଆନ୍ତୁ, ସୁରକ୍ଷିତ ଥାଆନ୍ତୁ! 🙏",
            "👋 **ନିଜ ଯତ୍ନ ନିଅନ୍ତୁ!** ଯେକୌଣସି ସ୍ୱାସ୍ଥ୍ୟ ପ୍ରଶ୍ନ ଥିଲେ ମୋ ପାଖକୁ ଆସନ୍ତୁ। 🙏",
        ],
    },
    "greeting": {
        "en": [
            "👋 {greeting}! I am your AI Health Assistant. How can I help you today?",
            "Hello! Need help with symptoms or disease information?"
        ],
        "hi": [
            "👋 {greeting}! मैं आपका स्वास्थ्य सहायक एआई हूँ। आज मैं आपकी कैसे मदद कर सकता हूँ?",
            "नमस्ते! क्या आपको स्वास्थ्य संबंधित जानकारी चाहिए?"
        ],
        "or": [
            "👋 {greeting}! ମୁଁ ଆପଣଙ୍କର ସ୍ୱାସ୍ଥ୍ୟ ସହାୟକ ଅଟେ। ଆଜି ମୁଁ କିପରି ସାହାଯ୍ୟ କରିବି?",
            "ନମସ୍କାର! ସ୍ୱାସ୍ଥ୍ୟ ସୂଚନା ଦରକାର କି?"
        ]
    },
    "general_query": {
        "en": [
            ("🤔 **I did not quite catch that — let me help guide you!**\n\n"
             "You can ask me about:\n"
             "• Disease **symptoms** — e.g., 'What are symptoms of malaria?'\n"
             "• **Vaccines** and immunization schedules\n"
             "• **Outbreak alerts** in your area\n"
             "• **Prevention** tips and hygiene advice\n"
             "• **Emergency** contacts\n\n"
             "Try rephrasing your question! 😊"),
            ("💬 **Hmm, I am not sure I understood that.**\n\n"
             "Here are some things I am great at:\n"
             "• 'What diseases cause fever and chills?'\n"
             "• 'Tell me about dengue prevention'\n"
             "• 'What vaccines are free for children?'\n"
             "• 'Any outbreak alerts near me?'\n\n"
             "Give it a try! 🌟"),
        ],
        "hi": [
            ("🤔 **मैं आपकी बात ठीक से समझ नहीं पाया।**\n\n"
             "आप मुझसे पूछ सकते हैं:\n"
             "• रोग के लक्षण (जैसे: मलेरिया के लक्षण)\n"
             "• टीकाकरण की जानकारी\n"
             "• प्रकोप अलर्ट\n"
             "• रोकथाम के उपाय"),
            ("💬 **मुझे समझ नहीं आया — क्या आप दोबारा पूछ सकते हैं?**\n\n"
             "उदाहरण: 'डेंगू के लक्षण क्या हैं?' या 'बच्चों के लिए कौन से टीके हैं?'"),
        ],
        "or": [
            ("🤔 **ମୁଁ ଆପଣଙ୍କ ପ୍ରଶ୍ନ ଠିକ ବୁଝି ପାରିଲିନି।**\n\n"
             "ଦୟାକରି ପ୍ରଶ୍ନ କରନ୍ତୁ:\n"
             "• ରୋଗ ଲକ୍ଷଣ\n"
             "• ଟୀକାକରଣ\n"
             "• ପ୍ରାଦୁର୍ଭାବ ଆଲର୍ଟ"),
        ],
    },
}

def generate_response_from_intent(intent: str, language: str) -> str:
    lang = language if language in ("en", "hi", "or") else "en"
    if intent in RESPONSE_VARIANTS:
        variants = RESPONSE_VARIANTS[intent].get(lang) or RESPONSE_VARIANTS[intent].get("en", [])
        if variants:
            chosen = random.choice(variants)
            if "{greeting}" in chosen:
                chosen = chosen.replace("{greeting}", _time_greeting(lang))
            return chosen
    responses = RESPONSES.get(intent, RESPONSES["general_query"])
    if isinstance(responses, dict):
        return responses.get(lang, responses.get("en", ""))
    return str(responses)


import datetime
import traceback

def build_disease_response_text(disease: str, language: str) -> str:
    info = DISEASE_INFO.get(disease, {})
    lang = language if language in ("en", "hi", "or") else "en"
    base = RESPONSES.get(
        disease.lower().replace("-", "").replace(" ", "_").replace("covid19", "covid"),
        RESPONSES["general_query"],
    )
    if isinstance(base, dict):
        return base.get(lang, base.get("en", ""))
    return str(base)

@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "ok",
        "models_loaded": all([intent_model, disease_bundle, lang_model]),
        "endpoints": [
            "/predict/intent",
            "/predict/disease",
            "/detect/language",
            "/predict/full",
            "/extract/symptoms",
            "/predict_image",
            "/chat",
        ],
    })

@app.route("/predict/intent", methods=["POST"])
def predict_intent():
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()
    if not message:
        return jsonify({"error": "message field is required"}), 400
    try:
        intent = intent_model.predict([message])[0]
        proba = intent_model.predict_proba([message])[0]
        confidence = float(np.max(proba))
        return jsonify({
            "intent": intent,
            "confidence": round(confidence, 4),
            "suggestions": SUGGESTIONS_MAP.get(intent, SUGGESTIONS_MAP["general_query"]),
            "model": "TF-IDF + Logistic Regression",
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/predict/disease", methods=["POST"])
def predict_disease():
    data = request.get_json(silent=True) or {}
    symptoms = data.get("symptoms", [])
    if not isinstance(symptoms, list) or not symptoms:
        return jsonify({"error": "symptoms must be a non-empty list"}), 400
    try:
        model = disease_bundle["model"]
        symptom_cols = disease_bundle["symptom_cols"]
        row = [0] * len(symptom_cols)
        for sym in symptoms:
            mapped = SYMPTOM_TO_DATASET_COL.get(sym)
            if mapped in symptom_cols:
                row[symptom_cols.index(mapped)] = 1
        X = [row]
        
        # Soft fallback if only 1 symptom is provided
        # But we still try to predict.
        proba = model.predict_proba(X)[0]
        idx = np.argmax(proba)
        pred_disease = model.classes_[idx]
        confidence = float(proba[idx])

        top_indices = np.argsort(proba)[::-1][:3]
        top_diseases = [{"disease": model.classes_[i], "probability": float(proba[i])} for i in top_indices]

        info = DISEASE_INFO.get(pred_disease, {})
        return jsonify({
            "disease": pred_disease,
            "confidence": round(confidence, 4),
            "prevention": info.get("prevention", "Consult a doctor."),
            "treatment": info.get("treatment", "Consult a doctor."),
            "helpline": info.get("helpline", "104"),
            "top_diseases": top_diseases
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/detect/language", methods=["POST"])
def detect_language():
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()
    if not message:
        return jsonify({"error": "message field is required"}), 400
    try:
        lang_code = lang_model.predict([message])[0]
        proba = lang_model.predict_proba([message])[0]
        confidence = float(np.max(proba))
        lang_names = {"en": "English", "hi": "Hindi", "or": "Odia"}
        return jsonify({
            "language": lang_code,
            "language_name": lang_names.get(lang_code, "Unknown"),
            "confidence": round(confidence, 4)
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/predict/full", methods=["POST"])
def predict_full():
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()
    if not message:
        return jsonify({"error": "message field is required"}), 400
    try:
        # Intent
        intent = intent_model.predict([message])[0]
        i_prob = intent_model.predict_proba([message])[0]
        int_conf = float(np.max(i_prob))

        # Format
        return jsonify({
            "message": message,
            "intent": {"name": intent, "confidence": round(int_conf, 4)},
            "suggestions": SUGGESTIONS_MAP.get(intent, SUGGESTIONS_MAP["general_query"])
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/extract/symptoms", methods=["POST"])
def extract_symptoms():
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip().lower()
    if not message:
        return jsonify({"error": "message required"}), 400
    found = []
    for symptom, syns in SYMPTOM_SYNONYMS.items():
        if any(re.search(r'\b' + re.escape(s) + r'\b', message) for s in syns):
            found.append(symptom)
        elif any(s in message for s in syns if len(s) > 3): # fallback for non-english
            # Be careful with short synonyms
            found.append(symptom)
    found = list(set(found))
    return jsonify({"symptoms": found, "count": len(found)})


@app.route("/predict_image", methods=["POST"])
def predict_image():
    if 'file' not in request.files:
        return jsonify({"error": "No file part in the request"}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
        
    try:
        file_bytes = file.read()
        prediction = predict_from_image(file_bytes)
        return jsonify(prediction)
    except Exception as e:
        import traceback
        return jsonify({"error": str(e), "traceback": traceback.format_exc()}), 500

@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()
    if not message:
        return jsonify({"error": "message field is required"}), 400

    try:
        # 1. Detect language
        lang = lang_model.predict([message])[0]
        lang_conf = float(np.max(lang_model.predict_proba([message])[0]))
        lang_names = {"en": "English", "hi": "Hindi", "or": "Odia"}

        # 2. Extract symptoms
        msg_lower = message.lower()
        symptoms_found = []
        for symptom, syns in SYMPTOM_SYNONYMS.items():
            if any(re.search(r'\b' + re.escape(s) + r'\b', msg_lower) for s in syns):
                symptoms_found.append(symptom)
            elif any(s in msg_lower for s in syns if len(s) > 3):
                symptoms_found.append(symptom)
        symptoms_found = list(set(symptoms_found))

        # 3. Detect Intent
        intent = intent_model.predict([message])[0]
        intent_conf = float(np.max(intent_model.predict_proba([message])[0]))

        if symptoms_found:
            intent = "disease_symptoms"

        # 4. Disease Prediction
        disease_prediction = None
        if symptoms_found:
            if len(symptoms_found) < 2:
                # Do not run the ML model for just 1 symptom to avoid scary predictions like AIDS.
                intent = "disease_symptoms"
                disease_prediction = None
                
                lang_msgs = {
                    "hi": f"आपने '{symptoms_found[0]}' का उल्लेख किया है, लेकिन यह एक बहुत ही सामान्य लक्षण है। कृपया मुझे सटीक बीमारी बताने के लिए 1-2 और लक्षण बताएं (जैसे 'बुखार के साथ सिरदर्द और उल्टी')।",
                    "or": f"ଆପଣ '{symptoms_found[0]}' ବିଷୟରେ କହିଛନ୍ତି, କିନ୍ତୁ ଏହା ଏକ ସାଧାରଣ ଲକ୍ଷଣ। ଦୟାକରି ସଠିକ୍ ରୋଗ ଜାଣିବା ପାଇଁ ଆଉ 1-2 ଟି ଲକ୍ଷଣ କୁହନ୍ତୁ (ଯେପରିକି 'ଜ୍ୱର ସହିତ ମୁଣ୍ଡବିନ୍ଧା ଓ ବାନ୍ତି')।",
                    "en": f"You mentioned '{symptoms_found[0]}', but this is a very common symptom. Please tell me 1-2 more symptoms (e.g. 'Fever with headache and vomiting') so I can predict the disease accurately."
                }
                
                if symptoms_found[0] == "fever":
                    lang_msgs = {
                        "hi": "🤒 अगर आपको केवल सामान्य बुखार है, तो आराम करें, खूब पानी पिएं और पेरासिटामोल लें। यदि बुखार 3 दिनों से अधिक रहता है या अन्य लक्षण (जैसे चकत्ते, उल्टी) दिखाई देते हैं, तो डॉक्टर से मिलें।",
                        "or": "🤒 ଯଦି ଆପଣଙ୍କୁ କେବଳ ସାଧାରଣ ଜ୍ୱର ଅଛି, ତେବେ ବିଶ୍ରାମ କରନ୍ତୁ, ପ୍ରଚୁର ପାଣି ପିଅନ୍ତୁ ଏବଂ ପାରାସିଟାମୋଲ ନିଅନ୍ତୁ। ଯଦି ଜ୍ୱର 3 ଦିନରୁ ଅଧିକ ରହେ କିମ୍ବା ଅନ୍ୟ ଲକ୍ଷଣ ଦେଖାଯାଏ, ତେବେ ଡାକ୍ତରଙ୍କ ପରାମର୍ଶ ନିଅନ୍ତୁ।",
                        "en": "🤒 If you are only experiencing a common fever, please rest, drink plenty of fluids, and you may consider taking Paracetamol. If the high temperature persists for more than 3 days or you develop other symptoms (like rashes, severe body ache, or vomiting), please consult a doctor immediately."
                    }
                elif symptoms_found[0] == "cough" or symptoms_found[0] == "sore_throat":
                    lang_msgs = {
                        "hi": "🗣️ सामान्य खांसी या गले में खराश के लिए, गर्म पानी पिएं, गरारे करें और आराम करें। यदि यह 1 सप्ताह से अधिक समय तक बनी रहती है, तो डॉक्टर से मिलें।",
                        "or": "🗣️ ସାଧାରଣ କାଶ କିମ୍ବା ଗଳା ଯନ୍ତ୍ରଣା ପାଇଁ, ଗରମ ପାଣି ପିଅନ୍ତୁ ଏବଂ ବିଶ୍ରାମ କରନ୍ତୁ। ଯଦି ଏହା 1 ସପ୍ତାହରୁ ଅଧିକ ସମୟ ରହେ, ତେବେ ଡାକ୍ତରଙ୍କ ପରାମର୍ଶ ନିଅନ୍ତୁ।",
                        "en": "🗣️ For a common cough or sore throat, drink warm fluids, try salt-water gargles, and rest. If it persists for more than a week or you develop a fever, please consult a doctor."
                    }
                    
                response_text = lang_msgs.get(lang, lang_msgs["en"])
            else:
                model = disease_bundle["model"]
                symptom_cols = disease_bundle["symptom_cols"]
                row = [0] * len(symptom_cols)
                for sym in symptoms_found:
                    mapped = SYMPTOM_TO_DATASET_COL.get(sym)
                    if mapped in symptom_cols:
                        row[symptom_cols.index(mapped)] = 1
                X = [row]
                proba_d = model.predict_proba(X)[0]
                idx_d = np.argmax(proba_d)
                cls_d = model.classes_[idx_d]
                conf_d = float(proba_d[idx_d])
    
                info = DISEASE_INFO.get(cls_d, {})
                disease_prediction = {
                    "disease": cls_d,
                    "confidence": conf_d,
                    "prevention": info.get("prevention", "Consult a doctor."),
                    "treatment": info.get("treatment", "Consult a doctor."),
                    "helpline": info.get("helpline", "104"),
                    "top_diseases": [{"disease": c, "probability": float(p)} for c, p in zip(model.classes_, proba_d) if p > 0.05][:3]
                }

        # 5. Generate Response Text
        llm_powered = False

        if disease_prediction is None and not symptoms_found:
            response_text = generate_response_from_intent(intent, lang)
        
        if disease_prediction:
            dm_name = disease_prediction["disease"]
            dm_conf = disease_prediction["confidence"] * 100
            dm_prev = disease_prediction["prevention"]
            dm_treat = disease_prediction["treatment"]
            dm_help = disease_prediction["helpline"]

            if sum(1 for _ in symptoms_found) == 1 and intent != "disease_symptoms":
                # Only 1 symptom, low confidence warning
                if lang == "hi":
                    response_text = "आपके लक्षण बहुत सामान्य हैं। मेरा सबसे अच्छा अनुमान नीचे दिया गया है, लेकिन कृपया उचित निदान के लिए डॉक्टर से मिलें।\n\n"
                    response_text += f"🦠 **संभावित रोग:** {dm_name}\n"
                elif lang == "or":
                    response_text = "ଆପଣଙ୍କ ଲକ୍ଷଣ ଅତି ସାଧାରଣ ଅଟେ। ମୋର ସର୍ବୋତ୍ତମ ଅନୁମାନ ତଳେ ଦିଆଯାଇଛି, କିନ୍ତୁ ଦୟାକରି ଡାକ୍ତରଙ୍କ ପରାମର୍ଶ ନିଅନ୍ତୁ।\n\n"
                    response_text += f"🦠 **ସମ୍ଭାବ୍ୟ ରୋଗ:** {dm_name}\n"
                else:
                    response_text = "Your symptoms are quite general. My best prediction is shown below, but please consult a doctor for a proper diagnosis.\n\n"
                    response_text += f"🦠 **Predicted Disease:** {dm_name}\n"
            else:
                if lang == "hi":
                    response_text = (
                        f"नमस्ते! आपने जो लक्षण बताए हैं, उनके आधार पर मुझे लगता है कि यह **{dm_name}** हो सकता है "
                        f"(मुझे {dm_conf:.0f}% विश्वास है)।\n\n"
                        f"🛡️ **सुरक्षित कैसे रहें:** {dm_prev}\n"
                        f"💊 **क्या करें:** {dm_treat}\n\n"
                        f"📞 कृपया डॉक्टर से सलाह लें, या जरूरत पड़ने पर हेल्पलाइन **{dm_help}** पर कॉल करें। अपना ख्याल रखें!"
                    )
                elif lang == "or":
                    response_text = (
                        f"ନମସ୍କାର! ଆପଣଙ୍କ ଲକ୍ଷଣ ଅନୁଯାୟୀ, ମୋର ଅନୁମାନ ଏହା **{dm_name}** ହୋଇପାରେ "
                        f"(ମୁଁ {dm_conf:.0f}% ନିଶ୍ଚିତ)।\n\n"
                        f"🛡️ **ପ୍ରତିରୋଧ:** {dm_prev}\n"
                        f"💊 **ଚିକିତ୍ସା:** {dm_treat}\n\n"
                        f"📞 ଦୟାକରି ଏକ ଡାକ୍ତର ଦେଖାନ୍ତୁ, କିମ୍ବା ଆବଶ୍ୟକ ହେଲେ **{dm_help}** ରେ କଲ କରନ୍ତୁ। ଭଲରେ ରୁହନ୍ତୁ!"
                    )
                else:
                    response_text = (
                        f"Hello there! Based on the symptoms you've shared, I estimate that you might be experiencing **{dm_name}** "
                        f"(I'm about {dm_conf:.0f}% sure).\n\n"
                        f"🛡️ **To stay safe:** {dm_prev}\n"
                        f"💊 **What you can do:** {dm_treat}\n\n"
                        f"📞 Remember, I'm just an AI! Please consult a real doctor soon, or call the health helpline at **{dm_help}** if you need immediate guidance. Take care!"
                    )

        llm_resp = generate_llm_response(
            user_message=message,
            intent=intent,
            language=lang,
            symptoms_found=symptoms_found,
            disease_prediction=disease_prediction,
        )
        if llm_resp:
            response_text = llm_resp
            llm_powered = True
        else:
            llm_powered = False
        
        return jsonify({
            "response": response_text,
            "intent": intent,
            "language": lang,
            "language_name": lang_names.get(lang, "Unknown"),
            "confidence": round(intent_conf, 4),
            "lang_confidence": round(lang_conf, 4),
            "symptoms_found": symptoms_found,
            "disease_prediction": disease_prediction,
            "suggestions": SUGGESTIONS_MAP.get(intent, SUGGESTIONS_MAP["general_query"]),
            "llm_powered": llm_powered
        })
        
    except Exception as e:
        import traceback
        return jsonify({"error": str(e), "traceback": traceback.format_exc()}), 500

if __name__ == "__main__":
    load_models()
    if GEMINI_API_KEY:
        print(f"🤖 Gemini LLM enabled  (model: {GEMINI_MODEL})")
    else:
        print("⚠️  Gemini API key NOT set — using template responses.")
        print("   To enable LLM-powered responses:")
        print("     Windows PS   : $env:GEMINI_API_KEY='your_key_here'")
        print("     Windows CMD  : set GEMINI_API_KEY=your_key_here")
        print("   Get a FREE key: https://aistudio.google.com/app/apikey")
    print("\n🚀 ML API Server starting on http://0.0.0.0:5002")
    print("   Endpoints:")
    print("   GET  /health")
    print("   POST /predict/intent")
    print("   POST /predict/disease")
    print("   POST /detect/language")
    print("   POST /predict/full")
    print("   POST /extract/symptoms")
    print("   POST /chat             ← all-in-one chatbot endpoint (LLM-powered when key set)")
    app.run(host="0.0.0.0", port=5002, debug=False)
