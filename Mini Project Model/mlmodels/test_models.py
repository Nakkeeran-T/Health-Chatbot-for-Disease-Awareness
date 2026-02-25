"""
Unit tests for all 3 ML models.
Run: python test_models.py
"""
import os
import sys
import unittest
import joblib
import pandas as pd
import numpy as np


# ─── Helpers ────────────────────────────────────────────────────────────────

def load_all_models():
    intent_model   = joblib.load("models/intent_classifier.pkl")
    disease_bundle = joblib.load("models/disease_predictor.pkl")
    lang_model     = joblib.load("models/lang_detector.pkl")
    return intent_model, disease_bundle, lang_model


# ═════════════════════════════════════════════════════════════════════════════
# TEST SUITE 1: Intent Classifier
# ═════════════════════════════════════════════════════════════════════════════
class TestIntentClassifier(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.model, _, _ = load_all_models()

    def _predict(self, text):
        return self.model.predict([text])[0]

    def _confidence(self, text):
        return float(np.max(self.model.predict_proba([text])[0]))

    # English queries
    def test_greeting_english(self):
        self.assertEqual(self._predict("hello"), "greeting")

    def test_malaria_english(self):
        self.assertEqual(self._predict("malaria symptoms and treatment"), "malaria")

    def test_dengue_english(self):
        self.assertEqual(self._predict("dengue fever prevention"), "dengue")

    def test_vaccine_english(self):
        self.assertEqual(self._predict("vaccination schedule for children"), "vaccine")

    def test_emergency_english(self):
        self.assertEqual(self._predict("ambulance number emergency"), "emergency")

    def test_tuberculosis_english(self):
        self.assertEqual(self._predict("tuberculosis tb symptoms"), "tuberculosis")

    def test_covid_english(self):
        self.assertEqual(self._predict("covid-19 symptoms prevention"), "covid")

    def test_cholera_english(self):
        self.assertEqual(self._predict("cholera watery diarrhea"), "cholera")

    def test_typhoid_english(self):
        self.assertEqual(self._predict("typhoid fever treatment"), "typhoid")

    def test_outbreak_english(self):
        self.assertEqual(self._predict("outbreak alert near me"), "outbreak_alert")

    def test_bye_english(self):
        self.assertEqual(self._predict("bye thank you"), "bye")

    # Hindi queries
    def test_greeting_hindi(self):
        self.assertEqual(self._predict("नमस्ते"), "greeting")

    def test_malaria_hindi(self):
        self.assertEqual(self._predict("मलेरिया के लक्षण"), "malaria")

    def test_vaccine_hindi(self):
        self.assertEqual(self._predict("टीकाकरण कार्यक्रम"), "vaccine")

    def test_emergency_hindi(self):
        self.assertEqual(self._predict("आपातकाल अस्पताल"), "emergency")

    # Odia queries
    def test_greeting_odia(self):
        self.assertEqual(self._predict("ହେଲୋ"), "greeting")

    def test_malaria_odia(self):
        self.assertEqual(self._predict("ମ୍ୟାଲେରିଆ ଲକ୍ଷଣ"), "malaria")

    def test_vaccine_odia(self):
        self.assertEqual(self._predict("ଟୀକାକରଣ"), "vaccine")

    # Confidence thresholds
    def test_high_confidence_malaria(self):
        conf = self._confidence("malaria fever chills mosquito")
        self.assertGreater(conf, 0.5, "Confidence should be > 50% for clear malaria query")

    def test_high_confidence_vaccine(self):
        conf = self._confidence("vaccine schedule immunization")
        self.assertGreater(conf, 0.5)

    # Dataset accuracy >= 80%
    def test_overall_accuracy_above_threshold(self):
        df = pd.read_csv("data/intent_training_data.csv", encoding="utf-8")
        preds = self.model.predict(df["text"])
        acc = (preds == df["intent"]).mean()
        print(f"\n   Intent Classifier — Training set accuracy: {acc*100:.1f}%")
        self.assertGreaterEqual(acc, 0.80, f"Accuracy {acc:.2%} is below 80% threshold")


# ═════════════════════════════════════════════════════════════════════════════
# TEST SUITE 2: Disease Predictor
# ═════════════════════════════════════════════════════════════════════════════
class TestDiseasePredictor(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        _, bundle, _ = load_all_models()
        cls.model        = bundle["model"]
        cls.symptom_cols = bundle["symptom_cols"]

    def _predict(self, symptoms_list):
        vec = {s: 0 for s in self.symptom_cols}
        for s in symptoms_list:
            key = s.lower().replace(" ", "_")
            if key in vec:
                vec[key] = 1
        X = pd.DataFrame([vec])
        return self.model.predict(X)[0]

    def _proba(self, symptoms_list):
        vec = {s: 0 for s in self.symptom_cols}
        for s in symptoms_list:
            key = s.lower().replace(" ", "_")
            if key in vec:
                vec[key] = 1
        X = pd.DataFrame([vec])
        return float(np.max(self.model.predict_proba(X)[0]))

    def test_malaria_prediction(self):
        result = self._predict(["fever", "chills", "headache", "nausea", "night_sweats"])
        self.assertEqual(result, "Malaria")

    def test_dengue_prediction(self):
        result = self._predict(["fever", "headache", "eye_pain", "rash", "body_pain", "bleeding"])
        self.assertEqual(result, "Dengue")

    def test_tuberculosis_prediction(self):
        result = self._predict(["cough", "blood_in_sputum", "night_sweats", "weight_loss", "chest_pain"])
        self.assertEqual(result, "Tuberculosis")

    def test_cholera_prediction(self):
        result = self._predict(["diarrhea", "vomiting", "dehydration", "muscle_cramps"])
        self.assertEqual(result, "Cholera")

    def test_covid_prediction(self):
        result = self._predict(["fever", "cough", "loss_of_taste", "loss_of_smell", "shortness_of_breath"])
        self.assertEqual(result, "COVID-19")

    def test_typhoid_prediction(self):
        result = self._predict(["fever", "abdominal_pain", "weight_loss", "rash", "diarrhea"])
        self.assertEqual(result, "Typhoid")

    def test_high_confidence_clear_case(self):
        conf = self._proba(["cough", "blood_in_sputum", "night_sweats", "weight_loss", "chest_pain", "fever"])
        self.assertGreater(conf, 0.5)

    def test_returns_valid_disease(self):
        valid_diseases = {"Malaria", "Dengue", "Tuberculosis", "Cholera", "COVID-19",
                          "Typhoid", "Hepatitis", "Influenza", "Measles", "Common Cold"}
        result = self._predict(["fever", "cough"])
        self.assertIn(result, valid_diseases)

    def test_overall_accuracy_above_threshold(self):
        df = pd.read_csv("data/disease_symptom_data.csv", encoding="utf-8")
        X  = df[[c for c in df.columns if c != "disease"]]
        y  = df["disease"]
        preds = self.model.predict(X)
        acc = (preds == y).mean()
        print(f"\n   Disease Predictor — Training set accuracy: {acc*100:.1f}%")
        self.assertGreaterEqual(acc, 0.80)


# ═════════════════════════════════════════════════════════════════════════════
# TEST SUITE 3: Language Detector
# ═════════════════════════════════════════════════════════════════════════════
class TestLanguageDetector(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        _, _, cls.model = load_all_models()

    def _predict(self, text):
        return self.model.predict([text])[0]

    def _confidence(self, text):
        return float(np.max(self.model.predict_proba([text])[0]))

    def test_english_detection(self):
        self.assertEqual(self._predict("I have fever and headache"), "en")

    def test_english_detection_2(self):
        self.assertEqual(self._predict("vaccine schedule for children"), "en")

    def test_hindi_detection(self):
        self.assertEqual(self._predict("मुझे बुखार है"), "hi")

    def test_hindi_detection_2(self):
        self.assertEqual(self._predict("टीकाकरण कार्यक्रम"), "hi")

    def test_odia_detection(self):
        self.assertEqual(self._predict("ମୋତେ ଜ୍ୱର ଅଛି"), "or")

    def test_odia_detection_2(self):
        self.assertEqual(self._predict("ଟୀକାକରଣ ସୂଚୀ"), "or")

    def test_confidence_english(self):
        conf = self._confidence("what are symptoms of malaria")
        self.assertGreater(conf, 0.5)

    def test_confidence_hindi(self):
        conf = self._confidence("मलेरिया के लक्षण क्या हैं")
        self.assertGreater(conf, 0.5)

    def test_confidence_odia(self):
        conf = self._confidence("ମ୍ୟାଲେରିଆ ଲକ୍ଷଣ")
        self.assertGreater(conf, 0.5)

    def test_overall_accuracy(self):
        df = pd.read_csv("data/language_detection_data.csv", encoding="utf-8")
        preds = self.model.predict(df["text"])
        acc = (preds == df["language"]).mean()
        print(f"\n   Language Detector  — Training set accuracy: {acc*100:.1f}%")
        self.assertGreaterEqual(acc, 0.90)


# ─── Runner ─────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    if not os.path.exists("models/intent_classifier.pkl"):
        print("❌ Models not found! Run: python train_models.py first.\n")
        sys.exit(1)

    print("="*60)
    print("🧪 Running ML Model Unit Tests")
    print("="*60)
    loader = unittest.TestLoader()
    suite  = unittest.TestSuite()
    suite.addTests(loader.loadTestsFromTestCase(TestIntentClassifier))
    suite.addTests(loader.loadTestsFromTestCase(TestDiseasePredictor))
    suite.addTests(loader.loadTestsFromTestCase(TestLanguageDetector))

    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    print("\n" + "="*60)
    if result.wasSuccessful():
        print("✅ ALL TESTS PASSED!")
    else:
        print(f"❌ {len(result.failures)} test(s) failed, {len(result.errors)} error(s)")
    print("="*60)
    sys.exit(0 if result.wasSuccessful() else 1)
