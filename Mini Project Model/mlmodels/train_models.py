"""
Train all 3 ML models for the AI Health Chatbot.
Models saved to models/ directory.

Run: python train_models.py
"""
import os
import sys

# Ensure stdout uses UTF-8 to prevent UnicodeEncodeError with emojis on Windows
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

import pandas as pd
import numpy as np
import joblib
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.pipeline import Pipeline
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.naive_bayes import MultinomialNB
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import (
    accuracy_score, classification_report, confusion_matrix
)
from sklearn.preprocessing import LabelEncoder

os.makedirs("models", exist_ok=True)
os.makedirs("plots", exist_ok=True)

# ─────────────────────────────────────────────────────────
# Ensure data exists
# ─────────────────────────────────────────────────────────
if not os.path.exists("data/intent_training_data.csv"):
    print("📦 Generating training data first...")
    import subprocess
    subprocess.run([sys.executable, "generate_data.py"], check=True)

# ═════════════════════════════════════════════════════════
# MODEL 1: INTENT CLASSIFIER
# ═════════════════════════════════════════════════════════
print("\n" + "="*60)
print("🧠 MODEL 1: Intent Classifier (TF-IDF + Logistic Regression)")
print("="*60)

df_intent = pd.read_csv("data/intent_training_data.csv", encoding="utf-8")
print(f"   Training samples: {len(df_intent)}")
print(f"   Intents: {df_intent['intent'].nunique()}")
print(f"   Intents list: {sorted(df_intent['intent'].unique())}")

X_intent = df_intent["text"]
y_intent = df_intent["intent"]

X_tr, X_te, y_tr, y_te = train_test_split(
    X_intent, y_intent, test_size=0.20, random_state=42, stratify=y_intent
)

intent_pipeline = Pipeline([
    ("tfidf", TfidfVectorizer(
        analyzer="char_wb",          # character n-grams work for multilingual
        ngram_range=(2, 5),
        max_features=15000,
        sublinear_tf=True,
        min_df=1,
    )),
    ("clf", LogisticRegression(
        C=5.0,
        max_iter=1000,
        multi_class="multinomial",
        solver="lbfgs",
        random_state=42,
    )),
])

intent_pipeline.fit(X_tr, y_tr)
y_pred = intent_pipeline.predict(X_te)
acc = accuracy_score(y_te, y_pred)
cv_scores = cross_val_score(intent_pipeline, X_intent, y_intent, cv=5)

print(f"\n   ✅ Test Accuracy         : {acc*100:.1f}%")
print(f"   ✅ Cross-val Accuracy    : {cv_scores.mean()*100:.1f}% ± {cv_scores.std()*100:.1f}%")
print("\n   Classification Report:")
print(classification_report(y_te, y_pred, zero_division=0))

# Confusion matrix plot
cm = confusion_matrix(y_te, y_pred, labels=sorted(y_intent.unique()))
plt.figure(figsize=(14, 10))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=sorted(y_intent.unique()),
            yticklabels=sorted(y_intent.unique()))
plt.title("Intent Classifier — Confusion Matrix", fontsize=14, fontweight='bold')
plt.ylabel("True Intent")
plt.xlabel("Predicted Intent")
plt.xticks(rotation=45, ha='right')
plt.tight_layout()
plt.savefig("plots/intent_confusion_matrix.png", dpi=150)
plt.close()
print("   📊 Confusion matrix saved → plots/intent_confusion_matrix.png")

joblib.dump(intent_pipeline, "models/intent_classifier.pkl")
print("   💾 Model saved → models/intent_classifier.pkl")

# ═════════════════════════════════════════════════════════
# MODEL 2: DISEASE SYMPTOM PREDICTOR
# ═════════════════════════════════════════════════════════
print("\n" + "="*60)
print("🩺 MODEL 2: Disease Symptom Predictor (Random Forest)")
print("="*60)

df_disease = pd.read_csv("dataset/Training_processed.csv", encoding="utf-8")
if 'Unnamed: 133' in df_disease.columns:
    df_disease = df_disease.drop('Unnamed: 133', axis=1)

print(f"   Training samples : {len(df_disease)}")
print(f"   Diseases         : {df_disease['prognosis'].nunique()}")
print(f"   Disease list     : {sorted(df_disease['prognosis'].unique())}")

symptom_cols = [c for c in df_disease.columns if c != "prognosis"]
X_disease = df_disease[symptom_cols]
y_disease = df_disease["prognosis"]

Xd_tr, Xd_te, yd_tr, yd_te = train_test_split(
    X_disease, y_disease, test_size=0.20, random_state=42, stratify=y_disease
)

disease_model = RandomForestClassifier(
    n_estimators=200,
    max_depth=None,
    min_samples_split=2,
    random_state=42,
    n_jobs=-1,
)
disease_model.fit(Xd_tr, yd_tr)
yd_pred = disease_model.predict(Xd_te)
acc_d = accuracy_score(yd_te, yd_pred)
cv_d = cross_val_score(disease_model, X_disease, y_disease, cv=5)

print(f"\n   ✅ Test Accuracy         : {acc_d*100:.1f}%")
print(f"   ✅ Cross-val Accuracy    : {cv_d.mean()*100:.1f}% ± {cv_d.std()*100:.1f}%")
print("\n   Classification Report:")
print(classification_report(yd_te, yd_pred, zero_division=0))

# Feature importance plot
importances = pd.Series(disease_model.feature_importances_, index=symptom_cols)
top_features = importances.sort_values(ascending=False).head(15)
plt.figure(figsize=(10, 6))
top_features.plot(kind='barh', color='teal')
plt.title("Top 15 Symptom Importances — Disease Predictor", fontsize=13, fontweight='bold')
plt.xlabel("Feature Importance")
plt.gca().invert_yaxis()
plt.tight_layout()
plt.savefig("plots/disease_feature_importance.png", dpi=150)
plt.close()
print("   📊 Feature importance saved → plots/disease_feature_importance.png")

# Save model + symptom column names (needed for inference)
joblib.dump({"model": disease_model, "symptom_cols": symptom_cols},
            "models/disease_predictor.pkl")
print("   💾 Model saved → models/disease_predictor.pkl")

# ═════════════════════════════════════════════════════════
# MODEL 3: LANGUAGE DETECTOR
# ═════════════════════════════════════════════════════════
print("\n" + "="*60)
print("🌐 MODEL 3: Language Detector (Char N-gram + Multinomial NB)")
print("="*60)

df_lang = pd.read_csv("data/language_detection_data.csv", encoding="utf-8")
print(f"   Training samples : {len(df_lang)}")
print(f"   Languages        : {sorted(df_lang['language'].unique())}")

X_lang = df_lang["text"]
y_lang = df_lang["language"]

Xl_tr, Xl_te, yl_tr, yl_te = train_test_split(
    X_lang, y_lang, test_size=0.20, random_state=42, stratify=y_lang
)

lang_pipeline = Pipeline([
    ("vec", CountVectorizer(
        analyzer="char_wb",
        ngram_range=(1, 4),
        max_features=10000,
        min_df=1,
    )),
    ("clf", MultinomialNB(alpha=0.1)),
])

lang_pipeline.fit(Xl_tr, yl_tr)
yl_pred = lang_pipeline.predict(Xl_te)
acc_l = accuracy_score(yl_te, yl_pred)
cv_l = cross_val_score(lang_pipeline, X_lang, y_lang, cv=5)

print(f"\n   ✅ Test Accuracy         : {acc_l*100:.1f}%")
print(f"   ✅ Cross-val Accuracy    : {cv_l.mean()*100:.1f}% ± {cv_l.std()*100:.1f}%")
print("\n   Classification Report:")
print(classification_report(yl_te, yl_pred, zero_division=0))

joblib.dump(lang_pipeline, "models/lang_detector.pkl")
print("   💾 Model saved → models/lang_detector.pkl")

# ═════════════════════════════════════════════════════════
# SUMMARY
# ═════════════════════════════════════════════════════════
print("\n" + "="*60)
print("✅ All models trained and saved successfully!")
print("="*60)
print(f"   Intent Classifier  → {acc*100:.1f}% accuracy")
print(f"   Disease Predictor  → {acc_d*100:.1f}% accuracy")
print(f"   Language Detector  → {acc_l*100:.1f}% accuracy")
print("\n   Files:")
print("   • models/intent_classifier.pkl")
print("   • models/disease_predictor.pkl")
print("   • models/lang_detector.pkl")
print("   • plots/intent_confusion_matrix.png")
print("   • plots/disease_feature_importance.png")
print("\n🚀 Start the Flask API: python ml_api_server.py")
