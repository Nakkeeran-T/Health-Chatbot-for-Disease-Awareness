import os
import numpy as np
import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D
from tensorflow.keras.models import Model
import joblib
import kagglehub

# Configuration
IMAGE_SIZE = (224, 224)
BATCH_SIZE = 32
EPOCHS = 10
MODEL_SAVE_PATH = "models/image_disease_model.h5"
CLASSES_SAVE_PATH = "models/image_classes.pkl"

# 1. Local Dataset Path
# Place your extracted image folders (Melanoma, Eczema, etc.) inside this directory
TRAIN_DIR = "dataset_image/IMG_CLASSES" 

print(f"🚀 Starting Model Training with Transfer Learning on {TRAIN_DIR}...")

# Ensure 'models/' directory exists
os.makedirs('models', exist_ok=True)

# 2. Load Data with Automatic Validation Split
# We add basic augmentation and reserve 20% for validation/testing
train_datagen = ImageDataGenerator(
    rescale=1./255,
    rotation_range=20,
    width_shift_range=0.2,
    height_shift_range=0.2,
    horizontal_flip=True,
    validation_split=0.2  # <--- Automatically split data!
)

print("📊 Loading Training Data...")
train_generator = train_datagen.flow_from_directory(
    TRAIN_DIR,
    target_size=IMAGE_SIZE,
    batch_size=BATCH_SIZE,
    class_mode='categorical',
    subset='training'
)

print("📊 Loading Validation Data...")
val_generator = train_datagen.flow_from_directory(
    TRAIN_DIR,  # Reusing TRAIN_DIR because validation_split does the work
    target_size=IMAGE_SIZE,
    batch_size=BATCH_SIZE,
    class_mode='categorical',
    subset='validation'
)

# Extract and save class map
classes = list(train_generator.class_indices.keys())
joblib.dump(classes, CLASSES_SAVE_PATH)
print(f"📦 Found classes: {classes}")

# 2. Build Model
print("🏗 Building MobileNetV2 architecture...")
base_model = MobileNetV2(weights='imagenet', include_top=False, input_shape=(224, 224, 3))
base_model.trainable = False  # Freeze base layers for speed

x = base_model.output
x = GlobalAveragePooling2D()(x)
x = Dense(128, activation='relu')(x)
predictions = Dense(len(classes), activation='softmax')(x)

model = Model(inputs=base_model.input, outputs=predictions)
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

# 3. Train
print("⚙️ Training model...")
model.fit(
    train_generator,
    epochs=EPOCHS,
    validation_data=val_generator
)

# 4. Save
print("💾 Saving the trained weights...")
model.save(MODEL_SAVE_PATH)
print("✅ Training complete! Model ready for API integration.")
