"""
Flask REST API server for ML model inference.
Exposes 4 endpoints for the Spring Boot backend to consume.

Start: python ml_api_server.py
Port : 5001
"""
import os
import sys
import joblib
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# ─── Load models ────────────────────────────────────────────────────────────
MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

intent_model = None
disease_bundle = None
lang_model = None

def load_models():
    global intent_model, disease_bundle, lang_model

    intent_path  = os.path.join(MODEL_DIR, "intent_classifier.pkl")
    disease_path = os.path.join(MODEL_DIR, "disease_predictor.pkl")
    lang_path    = os.path.join(MODEL_DIR, "lang_detector.pkl")

    missing = []
    for p, name in [(intent_path, "intent_classifier"), (disease_path, "disease_predictor"), (lang_path, "lang_detector")]:
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
    print("✅ All models loaded successfully!")

# ─── Suggestions by intent ──────────────────────────────────────────────────
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

# Disease prevention / treatment advice
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
}

# ─── Endpoints ──────────────────────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({
        "status": "ok",
        "models_loaded": all([intent_model, disease_bundle, lang_model]),
        "endpoints": ["/predict/intent", "/predict/disease", "/detect/language"],
    })


@app.route("/predict/intent", methods=["POST"])
def predict_intent():
    """
    Request body: { "message": "I have fever and chills" }
    Response:     { "intent": "malaria", "confidence": 0.87, "suggestions": [...] }
    """
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()

    if not message:
        return jsonify({"error": "message field is required"}), 400

    try:
        intent = intent_model.predict([message])[0]
        proba  = intent_model.predict_proba([message])[0]
        confidence = float(np.max(proba))

        return jsonify({
            "intent":      intent,
            "confidence":  round(confidence, 4),
            "suggestions": SUGGESTIONS_MAP.get(intent, SUGGESTIONS_MAP["general_query"]),
            "model":       "TF-IDF + Logistic Regression",
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/predict/disease", methods=["POST"])
def predict_disease():
    """
    Request body: { "symptoms": ["fever", "chills", "headache"] }
    Response:     { "disease": "Malaria", "confidence": 0.92, "prevention": "...", "treatment": "..." }
    """
    data = request.get_json(silent=True) or {}
    symptoms_input = data.get("symptoms", [])

    if not symptoms_input:
        return jsonify({"error": "symptoms list is required"}), 400

    try:
        model        = disease_bundle["model"]
        symptom_cols = disease_bundle["symptom_cols"]

        # Build binary feature vector
        feature_vec = {s: 0 for s in symptom_cols}
        for sym in symptoms_input:
            sym_clean = sym.lower().replace(" ", "_")
            if sym_clean in feature_vec:
                feature_vec[sym_clean] = 1

        import pandas as pd
        X = pd.DataFrame([feature_vec])
        disease    = model.predict(X)[0]
        proba      = model.predict_proba(X)[0]
        confidence = float(np.max(proba))

        info = DISEASE_INFO.get(disease, {})
        return jsonify({
            "disease":    disease,
            "confidence": round(confidence, 4),
            "prevention": info.get("prevention", "Consult a healthcare professional."),
            "treatment":  info.get("treatment",  "Visit the nearest PHC."),
            "helpline":   info.get("helpline",   "104"),
            "model":      "Random Forest",
            "top_diseases": [
                {"disease": cls, "probability": round(float(prob), 4)}
                for cls, prob in sorted(
                    zip(model.classes_, proba), key=lambda x: -x[1]
                )[:3]
            ],
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/detect/language", methods=["POST"])
def detect_language():
    """
    Request body: { "text": "मुझे बुखार है" }
    Response:     { "language": "hi", "confidence": 0.99, "language_name": "Hindi" }
    """
    data = request.get_json(silent=True) or {}
    text = data.get("text", "").strip()

    if not text:
        return jsonify({"error": "text field is required"}), 400

    try:
        lang       = lang_model.predict([text])[0]
        proba      = lang_model.predict_proba([text])[0]
        confidence = float(np.max(proba))

        lang_names = {"en": "English", "hi": "Hindi", "or": "Odia"}
        return jsonify({
            "language":      lang,
            "language_name": lang_names.get(lang, lang),
            "confidence":    round(confidence, 4),
            "model":         "Char N-gram + Multinomial Naive Bayes",
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/predict/full", methods=["POST"])
def predict_full():
    """
    All-in-one endpoint used by Spring Boot.
    Request body: { "message": "..." }
    Response:     { "intent": ..., "language": ..., "confidence": ..., "suggestions": [...] }
    """
    data = request.get_json(silent=True) or {}
    message = data.get("message", "").strip()

    if not message:
        return jsonify({"error": "message field is required"}), 400

    try:
        # Language detection
        lang       = lang_model.predict([message])[0]
        lang_proba = lang_model.predict_proba([message])[0]
        lang_conf  = float(np.max(lang_proba))

        # Intent detection
        intent       = intent_model.predict([message])[0]
        intent_proba = intent_model.predict_proba([message])[0]
        intent_conf  = float(np.max(intent_proba))

        return jsonify({
            "intent":      intent,
            "language":    lang,
            "confidence":  round(intent_conf, 4),
            "lang_confidence": round(lang_conf, 4),
            "suggestions": SUGGESTIONS_MAP.get(intent, SUGGESTIONS_MAP["general_query"]),
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    load_models()
    print("\n🚀 ML API Server starting on http://0.0.0.0:5001")
    print("   Endpoints:")
    print("   GET  /health")
    print("   POST /predict/intent")
    print("   POST /predict/disease")
    print("   POST /detect/language")
    print("   POST /predict/full")
    app.run(host="0.0.0.0", port=5001, debug=False)
